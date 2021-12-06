<script lang="ts" setup="">

import {defineProps, reactive, ref} from "vue";

defineProps(['msg'])

let display = ref(false);
let place = reactive({
  left: '0px',
  top: '0px',
})
let timeout: any;



function enter(event: MouseEvent) {
  if (!timeout) {
    timeout = setTimeout(() => {
        place.left = event.clientX + "px";
        place.top = event.clientY + "px";
        display.value = true;
        timeout = null;
    }, 200);
  }
}

function leave() {
  clearTimeout(timeout);
  timeout = null;
  display.value = false;
}

</script>

<template>
  <transition enter-active-class="scale-in-center" leave-active-class="scale-out-center" mode="out-in">
    <div v-show="display" :style="place" class="hover-pop">
     {{msg}}
    </div>
  </transition>
  <div style="cursor: pointer" @mouseenter="enter" @mouseleave="leave">
    <slot></slot>
  </div>
</template>

<style lang="scss" scoped>
.hover-pop {
  padding: 10px;
  max-width: 200px;
  font-size: 16px;
  text-indent: 10px;
  position: absolute;
  z-index: 1000;
  background-color: white;
  color: black;
  border-radius: 10px;
  transform: translate(-50%,-150%) scale(1);
}

.scale-in-center {
  animation: scale-in-center 0.1s cubic-bezier(0.250, 0.460, 0.450, 0.940) both;
}
@keyframes scale-in-center {
  0% {
    transform: translate(-50%,-150%) scale(0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%,-150%) scale(1);
    opacity: 1;
  }
}
.scale-out-center {
  animation: scale-out-center .1s cubic-bezier(0.550, 0.085, 0.680, 0.530) both;
}
@keyframes scale-out-center {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}


</style>
