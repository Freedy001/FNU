<script lang="ts" setup="">

import {defineComponent, reactive} from "vue";
import DivideLine from "../components/DivideLine.vue";

defineComponent({
  DivideLine,
})


let data = reactive({
  reverseProxyPort: 1,
  reverseProxyLbName: 'round-robin',
  proxyServerAddress:[
    {
      address:'127.0.0.1',
      port:4
    },
    {
      address:'12.0.1.1',
      port:3
    },
    {
      address:'5.12.0.1',
      port:2
    }
  ]
});

</script>

<template>
  <div class="data">
    <h2>基本信息:</h2>
    <div class="item">
      <span class="config-name">启用端口</span>
      <input type="text" class="item-input start-port" v-model="data.reverseProxyPort">
    </div>
    <div class="item">
      <span class="config-name">负载均衡名称</span>
      <input type="text" class="item-input start-port" v-model="data.reverseProxyLbName">
    </div>
    <h2 style="margin-top: 10px">配置组信息:</h2>
    <div class="config">
      <div v-for="config in data.proxyServerAddress">
        <div class="item">
          <span class="config-group-name">被代理服务地址</span>
          <input class="item-input" style="width: 200px" type="text" v-model="config.address">
          <span class="config-group-name">被代理服务端口</span>
          <input class="item-input" style="width: 200px" type="text" v-model="config.port">
        </div>
        <DivideLine></DivideLine>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

.data {
  max-width: 1000px;
  height: 80%;
  width: 100%;
  overflow: auto;

  &::-webkit-scrollbar {
    /*滚动条整体样式*/
    width: 0; /*高宽分别对应横竖滚动条的尺寸*/
    height: 0;
  }

  span {
    text-align: center;
    margin-right: 20px;
  }


  .item {
    margin-right: 10px;
    height: 45px;
    display: flex;
    align-items: center;
    justify-content: center;

    .config-name {
      flex: 1;
    }

    .start-port {
      flex: 3;
    }

    .config-group-name {
      width: 150px;
      flex: 1;
    }

    .item-input {
      color: white;
      background: #3c4679;
      border-radius: 10px;
      height: 80%;
      width: 45%;
      outline: none;
      font-size: 15px;
      text-align: center;
      border: none;

      &:focus {
        border: none;
      }
    }


  }

  .config {
    margin-top: 10px;

    .item {
      justify-content: space-around;

      .item-input {
        flex: 1;
      }

      .item-input.remote-port {
        flex: 3;
      }

    }

  }

}

</style>
