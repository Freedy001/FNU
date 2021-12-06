import axios from 'axios'


const baseURL = import.meta.env.DEV ? "http://localhost:12/root" : "/root"
const ResourceURL = import.meta.env.DEV ? "http://localhost:12" : ""

export interface ILogin {
    username: string,
    password: string
}




export async function get(uri: string) {
    const {data} = await axios.get(baseURL + convertUri(uri));
    return data
}

export async function post(uri: string, dataFiled: any) {
    const {data} = await axios.post(baseURL + convertUri(uri), dataFiled);
    return data
}

function convertUri(uri: string) {
    let resultUri: string = "";
    uri.split("/").forEach(uriBlock => {
        if (uriBlock != "") {
            resultUri += "/" + uriBlock
        }
    })
    return resultUri;
}

export function loadResource(uri: any) {
    if (("" + uri).startsWith("http")) {
        return uri;
    } else {
        return ResourceURL + uri;
    }
}

export async function getFrontApi(uri: string) {
    const {data} = await axios.get(ResourceURL + uri);
    return data;
}
