<script lang="ts" setup="">
import {defineEmits, defineProps, getCurrentInstance, reactive, ref, watch} from "vue";

defineProps(['disable'])
defineEmits(['click'])
let {proxy} = getCurrentInstance();
let dynamicClass = ref([proxy.disable ? "btn-style-dis" : "not-btn-style-dis"])


watch(() => proxy.disable, () => {
  dynamicClass.value[0] = proxy.disable ? "dis" : "not-dis";
})

let logBtnStyle = reactive({
  transform: 'scale(1)',
  backgroundColor:proxy.disable?'#6b6b6b':'none'
})

function clickBtn(event: any) {
  if (!proxy.disable) {
    proxy.$emit('click', event);
  } else {
    if (!dynamicClass.value[1]) {
      dynamicClass.value[1] = 'shake'
      setTimeout(() => {
        dynamicClass.value.length = 1;
      }, 600)
    }
  }
}

function up() {

}
function down() {

}
</script>

<template>
  <div class="btn-style"
       @click="clickBtn($event)"
       :class="dynamicClass"
       :style="logBtnStyle"
       @mousedown="logBtnStyle.transform=disable?'scale(1)':'scale(1.05)'"
       @mouseup="logBtnStyle.transform=disable?'scale(1)':'scale(1)'">
    <div class="center">
      <slot></slot>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.btn-style{
  text-align: center;
  user-select: none;
  .center{
    position: relative;
    left: 50%;
    top: 50%;
    transform: translate(-50%,-50%);
  }
}
.btn-style.btn-style-dis {
  cursor: no-drop;
}

.not-btn-style-dis {
  cursor: pointer;

  &:hover {
    color: #acacf1;
    background-color: #101015;
  }
}

.shake {
  animation: shake-horizontal 0.6s cubic-bezier(0.455, 0.030, 0.515, 0.955) both;
}

@keyframes shake-horizontal {
  0%,
  100% {
    transform: translateX(0);
  }
  10%,
  30%,
  50%,
  70% {
    transform: translateX(-10px);
  }
  20%,
  40%,
  60% {
    transform: translateX(10px);
  }
  80% {
    transform: translateX(8px);
  }
  90% {
    transform: translateX(-8px);
  }
}
</style>
