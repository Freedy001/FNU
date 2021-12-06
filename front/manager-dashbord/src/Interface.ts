export interface IIntranet {
    isLocalStart: boolean,
    intranetChannelCacheMinSize: number,
    intranetChannelCacheMaxSize: number,
    intranetGroups: IConfigGroup[] | null,
    portChannelCacheLbName: string,
    intranetChannelRetryTimes: number,
    intranetReaderIdleTimes: number,
    intranetReaderIdleTime: number,
    intranetMaxBadConnectTimes: number,
    intranetServerZeroChannelIdleTime: number,
    startTime:number,
}

export interface IIntranetRemote{
    isRemoteStart: boolean,
    intranetRemotePort: number,
}

export interface IConfigGroup {
    localServerAddress: string,
    localServerPort: number,
    remoteAddress: string,
    remotePort: number,
    remoteServerPort: number,
}

export interface IReverseProxy {
    reverseProxyPort: number,
    reverseProxyLbName: string,
}

export interface IJumpServer {
    jumpLocalPort: number,
    jumpRemoteLbName: string,
    jumpRemotePort: number,
}


export interface IManager {
    managePort: number,
    manageUsername: string,
    managePassword: string,
}

export interface IEncrypt {
    authenticationTime: number,
}


export function copyProperties(source: any, dest: any) {
    Object.keys(source).forEach(key => {
        dest[key] = source[key];
    })
}