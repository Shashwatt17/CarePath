import type {Metadata} from "next";
import {PublicShare} from "@/components/sharing/public-share";
export const metadata:Metadata={title:"CarePath · Shared Visit Pack",robots:{index:false,follow:false,nocache:true},referrer:"no-referrer"};
export default function Page(){return <PublicShare/>;}
