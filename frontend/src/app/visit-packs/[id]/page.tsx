import { PackBuilder } from "@/components/visit-packs/pack-builder";
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <PackBuilder key={id} packId={id}/>;}
