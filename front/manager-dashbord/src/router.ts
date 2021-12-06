import {createRouter, createWebHashHistory, RouteRecordRaw} from "vue-router";

const routes: Array<RouteRecordRaw> = [
    {
        path: "/",
        redirect: "/DashBord",
    },
    {
        path: "/DashBord",
        component: () => import('./view/DashBord.vue'),
    },
    {
        path: "/intranetLocal",
        component: () => import('./view/IntranetLocal.vue'),
    },
    {
        path: "/intranetRemote",
        component: () => import('./view/IntranetRemote.vue'),
    },
    {
        path: "/ReverseProxy",
        component: () => import('./view/ReverseProxy.vue'),
    },
    {
        path: "/HttpProxy",
        component: () => import('./view/HttpProxy.vue'),
    },
    {
        path: "/Encrypt",
        component: () => import('./view/Encrypt.vue'),
    },
    {
        path: "/Manager",
        component: () => import('./view/Manager.vue'),
    },
    // {
    //     path: "/index",
    //     component:()=>import('../view/Index.vue'),
    //     children:[
    //         {
    //             path:"",
    //             component:()=>import('../view/Dashboard.vue'),
    //             meta:{
    //                 isKeep: false,
    //             }
    //         },
    //     ]
    // }

];

const router = createRouter({
    history: createWebHashHistory(),
    routes
})


export default router
