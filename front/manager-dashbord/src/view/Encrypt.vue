<script lang="ts" setup="">

import {defineComponent, reactive} from "vue";
import DivideLine from "../components/DivideLine.vue";
import question from '../assets/question.svg'
import HoverPop from '../components/HoverPop.vue'

defineComponent({
  DivideLine,
  HoverPop,
})


let data = reactive({
  authenticationTime: 1
});

</script>

<template>
  <div class="data">
    <h2>基本信息:</h2>
    <div class="item">
      <div style="margin-right: 20px;display: flex;justify-content: center;align-items: center" class="config-name">
        <span style="margin-right: 0">密钥</span>
        <HoverPop msg="为了安全起见，密钥仅支持在配置文件中修改！">
          <img :src="question" class="dot" alt="">
        </HoverPop>
      </div>
      <div style="user-select: none" class="item-input start-port">
        <span style="position: relative; margin: 0;top: 30%;">
          ***************************************
        </span>
      </div>
    </div>
    <div class="item">
      <span class="config-name">认证信息加密次数</span>
      <input type="text" class="item-input start-port" v-model="data.authenticationTime">
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
      margin-right: 0;

      &:focus {
        border: none;
      }
    }


  }
}

.dot {
  display: inline-block;
  width: 15px;
  height: 15px;
  border-radius: 100%;
  margin-right: 10px;
  margin-left: 10px;
}

</style>
