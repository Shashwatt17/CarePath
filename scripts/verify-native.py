from pathlib import Path
import os,subprocess,time,json,secrets,requests,hashlib,sys,tempfile,argparse
parser=argparse.ArgumentParser(description="Start isolated native services and verify CarePath with synthetic records.")
parser.add_argument('--postgres-bin',required=True,type=Path)
parser.add_argument('--redis-bin',required=True,type=Path)
parser.add_argument('--java',default='java')
args=parser.parse_args()
ROOT=Path(__file__).resolve().parents[1]; R=Path(tempfile.mkdtemp(prefix='carepath-native-')); PG=args.postgres_bin.resolve(); JAVA=args.java
subprocess.run([PG/'initdb','-D',R/'postgres','-U','carepath','--auth-local=trust','--auth-host=trust'],check=True,stdout=(R/'initdb.log').open('w'))
with (R/'postgres/postgresql.conf').open('a') as f:f.write("\nunix_socket_directories = ''\n")
print('Temporary verification workspace: '+str(R),flush=True)

processes=[];results=[]
def start(args,name,cwd=None,env=None):
    p=subprocess.Popen([str(x) for x in args],cwd=cwd,env=env,stdout=(R/(name+'.log')).open('a'),stderr=subprocess.STDOUT);processes.append(p);return p

def passed(name):
    results.append(name);print('PASS '+name,flush=True);(R/'verification.json').write_text(json.dumps({'passed':results},indent=2))
def wait_http(url,p):
    for _ in range(120):
        if p.poll() is not None:raise RuntimeError('Service exited before readiness: '+url)
        try:
            if requests.get(url,timeout=2).status_code==200:return
        except requests.RequestException:pass
        time.sleep(1)
    raise RuntimeError('Readiness timeout '+url)
def stop(p):
    p.terminate()
    try:p.wait(timeout=30)
    except subprocess.TimeoutExpired:p.kill();p.wait()
try:
    pg=start([PG/'postgres','-D',R/'postgres','-h','127.0.0.1','-p','55432'],'postgres-native')
    for _ in range(30):
        check=subprocess.run([PG/'pg_isready','-h','127.0.0.1','-p','55432'],capture_output=True)
        if check.returncode==0:break
        time.sleep(.2)
    subprocess.run([PG/'createdb','-h','127.0.0.1','-p','55432','-U','carepath','carepath'],check=True)
    pgpass=secrets.token_hex(24); redispass=secrets.token_hex(24)
    subprocess.run([PG/'psql','-h','127.0.0.1','-p','55432','-U','carepath','-d','carepath'],input=f"ALTER USER carepath WITH PASSWORD '{pgpass}';",text=True,check=True,stdout=subprocess.DEVNULL)
    hba=R/'postgres/pg_hba.conf'; hba.write_text(hba.read_text().replace('trust','scram-sha-256'));pg.send_signal(__import__('signal').SIGHUP)
    (R/'redis.conf').write_text(f'bind 127.0.0.1\nport 56379\nrequirepass {redispass}\ndir {R}\nappendonly yes\n')
    os.chmod(R/'redis.conf',0o600)
    redis=start([args.redis_bin.resolve(),R/'redis.conf'],'redis-native')
    if not (R/'private.pem').exists():
        subprocess.run(['openssl','genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:2048','-out',str(R/'private.pem')],check=True,stderr=subprocess.DEVNULL)
        subprocess.run(['openssl','pkey','-in',str(R/'private.pem'),'-pubout','-out',str(R/'public.pem')],check=True)
    os.chmod(R/'private.pem',0o600)
    env=os.environ.copy(); env.update({'DATABASE_URL':'jdbc:postgresql://127.0.0.1:55432/carepath?currentSchema=carepath','SPRING_FLYWAY_SCHEMAS':'carepath','POSTGRES_USER':'carepath','POSTGRES_PASSWORD':pgpass,'REDIS_HOST':'127.0.0.1','REDIS_PORT':'56379','REDIS_PASSWORD':redispass,'AUTH_RATE_KEY':secrets.token_hex(32),'JWT_PRIVATE_KEY_PATH':str(R/'private.pem'),'JWT_PUBLIC_KEY_PATH':str(R/'public.pem'),'AUTH_COOKIE_SECURE':'false','FRONTEND_ORIGIN':'http://localhost:3000','STORAGE_BACKEND':'database','STORAGE_ROOT':str(R/'documents'),'PROCESSING_TEMP_ROOT':str(R/'processing'),'SERVER_PORT':'8080','LLM_ENABLED':'false'})
    jar=ROOT/'backend/target/carepath-backend-0.1.0-SNAPSHOT.jar'
    def backend():return start([JAVA,'-Xmx192m','-XX:+UseSerialGC','-jar',jar],'backend-native',ROOT/'backend',env)
    app=backend();wait_http('http://127.0.0.1:8080/actuator/health/readiness',app);passed('Native PostgreSQL/Flyway V1-V14 and authenticated Redis readiness')
    front=start(['node','node_modules/next/dist/bin/next','start','--hostname','127.0.0.1','--port','3000'],'frontend-native',ROOT/'frontend',env)
    wait_http('http://127.0.0.1:3000/login',front);passed('Production Next.js starts against native backend')
    api='http://127.0.0.1:3000/api/v1';s=requests.Session();s.headers.update({'Origin':'http://localhost:3000','X-CarePath-Client':'web'})
    def req(method,path,expected=200,**kw):
        r=s.request(method,api+path,timeout=150,**kw)
        if r.status_code!=expected:raise AssertionError(f'{method} {path}: expected {expected}, got {r.status_code}: '+r.text[:300])
        return r
    cred={'email':'native-'+secrets.token_hex(5)+'@example.invalid','password':secrets.token_urlsafe(24)}
    req('POST','/auth/register',201,json={**cred,'displayName':'Synthetic Native Verification'})
    login=req('POST','/auth/login',json=cred).json();s.headers['Authorization']='Bearer '+login['accessToken'];req('GET','/auth/me');passed('Registration and login through Next proxy with real Redis')
    docs=[];observations=[]
    for file in ['january-lab.pdf','april-lab.pdf','september-lab.pdf','scanned-lab.pdf']:
        data=(ROOT/'sample-data/phase4'/file).read_bytes()
        doc=req('POST','/documents',201,files={'file':(file,data,'application/pdf'),'metadata':('',json.dumps({'documentType':'LAB_REPORT','tags':[]}), 'application/json')}).json();did=doc['id'];docs.append((did,data))
        req('POST',f'/documents/{did}/process',202)
        for _ in range(100):
            state=req('GET',f'/documents/{did}/processing-status').json()
            if state['state']!='PROCESSING':break
            time.sleep(1)
        assert state['state'] in ['NEEDS_REVIEW','COMPLETED'],state
        candidates=req('GET',f'/review/candidates?documentId={did}&size=100').json()['items']
        assert candidates,'No extracted observations'
        for c in candidates:
            if c['decision']['canConfirm']:
                obs=req('POST',f"/review/candidates/{c['id']}/confirm",json={'version':c['version']}).json()['observation'];observations.append(obs)
        assert req('GET',f'/documents/{did}/download').content==data
        assert req('GET',f'/documents/{did}/preview').headers['Content-Type']=='image/png'
        passed(file+' upload, real extraction, review, preview and identical download')
    events=req('GET','/history/events?size=100').json();assert events['items'];passed('Trusted observations appear in native PostgreSQL timeline')
    answer=req('POST','/assistant/ask',json={'question':'How has my hemoglobin changed?','useAi':False}).json()
    assert answer['mode']=='DETERMINISTIC' and answer['evidence'] and '12.1' in json.dumps(answer),answer
    passed('Evidence-grounded explanation from verified measurements')
    fallback=req('POST','/assistant/ask',json={'question':'How has my hemoglobin changed?','useAi':True}).json();assert fallback['providerStatus']=='NOT_CONFIGURED' and fallback['facts'];passed('Unconfigured LLM falls back explicitly without losing facts')
    profile={'introduction':'SYNTHETIC DEMO PATIENT','medicalHistory':'Synthetic history for verification','medications':'Patient-reported example','allergies':'Not provided','reasonForVisit':'Review selected reports'}
    assert req('PUT','/patient-profile',json=profile).json()==profile
    assert req('GET','/patient-profile').json()==profile
    pack=req('POST','/visit-packs',201,json={'title':'Synthetic patient summary','reasonForVisit':'Review reports','items':[{'type':'PROFILE'},{'type':'DOCUMENT','sourceId':docs[0][0]}],'version':0}).json()
    pack_id=pack['id'];preview=req('GET',f'/visit-packs/{pack_id}/preview').json()
    frozen=req('POST',f'/visit-packs/{pack_id}/generate',json={'version':pack['version'],'previewHash':preview['hash']}).json()
    assert profile['introduction'] in json.dumps(frozen['snapshot'])
    assert req('GET',f'/visit-packs/{pack_id}/pdf').content.startswith(b'%PDF')
    share=req('POST','/shares',201,json={'packId':pack_id,'expiryMinutes':15,'includeReports':True}).json()
    anon=requests.Session();token=share['token']
    assert anon.post(api+'/public/share/access',json={'token':token},timeout=30).status_code==200
    download=anon.post(api+'/public/share/report',json={'token':token,'itemIndex':1,'evidenceIndex':0},timeout=30)
    assert download.status_code==200 and download.content==docs[0][1]
    assert 'no-store' in download.headers.get('Cache-Control','').split(', '), download.headers.get('Cache-Control')
    req('POST',f"/shares/{share['share']['id']}/revoke")
    assert anon.post(api+'/public/share/report',json={'token':token,'itemIndex':1,'evidenceIndex':0},timeout=30).status_code==404
    passed('Patient profile, immutable PDF snapshot, anonymous selected-original download and revocation on PostgreSQL')
    # Original bearer token, keys and data must remain usable after all three backend services restart.
    stop(app);stop(redis);stop(pg)
    pg=start([PG/'postgres','-D',R/'postgres','-h','127.0.0.1','-p','55432'],'postgres-restart')
    redis=start([args.redis_bin.resolve(),R/'redis.conf'],'redis-restart')
    app=backend();wait_http('http://127.0.0.1:8080/actuator/health/readiness',app)
    req('GET','/auth/me')
    assert req('GET','/patient-profile').json()==profile
    assert req('GET',f'/visit-packs/{pack_id}').json()['snapshot']==frozen['snapshot']
    for did,data in docs:assert req('GET',f'/documents/{did}/download').content==data
    assert req('GET','/history/events?size=100').json()['items']
    passed('Backend + PostgreSQL + Redis restart preserves session, PDF bytes and timeline')
    did,data=docs[0];req('DELETE',f'/documents/{did}',204);req('GET',f'/documents/{did}/download',404);passed('Deletion removes durable original and denies subsequent download')
    (ROOT/'docs/verification/native-runtime-latest.json').write_text(json.dumps({'passed':results},indent=2))
    print('NATIVE WORKFLOW VERIFIED',flush=True)
except Exception as e:
    (R/'verification.json').write_text(json.dumps({'passed':results,'failure':str(e)},indent=2));print('FAILED '+str(e),flush=True);raise
finally:
    for p in reversed(processes):
        if p.poll() is None:stop(p)
