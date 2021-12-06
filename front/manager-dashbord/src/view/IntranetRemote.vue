<script lang="ts" setup="">

import {defineComponent, reactive} from "vue";
import DivideLine from "../components/DivideLine.vue";

defineComponent({
  DivideLine,
})


let data = reactive({
  intranetRemotePort: 1234,
  intranetGroups: [
    {
      localServerAddress: '127.0.0.1',
      localServerPort: 4567,
      remoteAddress: '1237.0.0.1',
      remotePort: 7777,
      remoteServerPort: 8080,
    },
    {
      localServerAddress: '127.0.0.1',
      localServerPort: 4567,
      remoteAddress: '1237.0.0.1',
      remotePort: 7777,
      remoteServerPort: 8080,
    },
    {
      localServerAddress: '127.0.0.1',
      localServerPort: 4567,
      remoteAddress: '1237.0.0.1',
      remotePort: 7777,
      remoteServerPort: 8080,
    },
    {
      localServerAddress: '127.0.0.1',
      localServerPort: 4567,
      remoteAddress: '1237.0.0.1',
      remotePort: 7777,
      remoteServerPort: 8080,
    }
  ],
});

</script>

<template>
<div class="data">
  <h2>基本信息:</h2>
  <div class="item">
    <span class="config-name">启用端口</span>
    <input type="text" class="item-input start-port" v-model="data.intranetRemotePort">
  </div>
  <h2 style="margin-top: 10px">配置组信息:</h2>
  <div class="config">
    <div v-for="config in data.intranetGroups">
      <div class="item">
        <span class="config-group-name">本地服务地址</span>
        <input class="item-input" style="width: 200px" type="text" v-model="config.localServerAddress">
        <span class="config-group-name">本地服务端口</span>
        <input class="item-input" style="width: 200px" type="text" v-model="config.localServerPort">
      </div>
      <div class="item">
        <span class="config-group-name">远程服务地址</span>
        <input class="item-input" style="width: 200px" type="text" v-model="config.remoteAddress">
        <span class="config-group-name">本地服务端口</span>
        <input class="item-input" style="width: 200px" type="text" v-model="config.remotePort">
      </div>
      <div class="item">
        <span class="config-group-name">远程提供服务地址</span>
        <input class="item-input remote-port" style="width: 75%" type="text" v-model="config.remoteServerPort">
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
    margin-top: 10px;
    margin-right: 10px;
    height: 45px;
    display: flex;
    align-items: center;
    justify-content: center;

    .config-name {
     flex: 1;
    }

    .start-port{
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
