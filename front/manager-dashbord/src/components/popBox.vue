<!--suppress JSUnresolvedVariable -->
<template>
	<FullScreen v-if="opacity" :opacity="opacityNum" @clickOutSide="handleDismiss">
		<div class="pop-card" :style="cardStyle" :class="sizeClass">
			<slot></slot>
		</div>
	</FullScreen>
</template>

<script setup lang="ts">
//入场动画
import {defineComponent, defineProps, getCurrentInstance, reactive, ref, watch} from "vue";
import FullScreen from "./FullScreen.vue";

defineProps(['startX', 'startY','size'])
const {proxy}: any = getCurrentInstance();
defineComponent({
	FullScreen
})
// onMounted(()=>{
// 	if (isDarkMode()){
// 		sizeClass['dark']=true;
// 	}
// })
//加载size
let sizeClass=reactive<any>({})
//初始化弹窗出现位置
let cardStyle = reactive<any>({})
let opacityNum = ref(0.3)
let opacity = ref(false)
watch(() => proxy.startX, () => {
	cardStyle['left'] = proxy.startX + 'px'
	cardStyle['top'] = proxy.startY + 'px'
	opacityNum.value = 0.3
	//入场动画
	sizeClass[`startAnimate${proxy.size}`]=true
	opacity.value = true
})

function handleDismiss() {
	opacityNum.value = 0
	//出场动画
	sizeClass[`endAnimate${proxy.size}`]=true
	setTimeout(() => {
		opacity.value = false
		sizeClass[`startAnimate${proxy.size}`]=false
		sizeClass[`endAnimate${proxy.size}`]=false
	}, 500)
}
</script>

<style scoped lang="scss">
.pop-card {
	position: absolute;
  overflow: hidden;
	top: 0;
	left: 0;
	width: 0;
	transform: translate(-50%, -50%);
	border-radius:10px;
	box-shadow: none;
	border: none;

	:deep(.el-card__body){
		padding: 0;
	}
}

//small大小
.startAnimateSmall {
	animation: showDescSmall 0.5s ease both;
}

@keyframes showDescSmall {
	0% {
		width: 0;
		height: 0;
		border-radius: 50%;
		background-color: #3a9ff5;
	}
	25% {
		border-radius: 10px;
	}
	50% {
		left: 50%;
		top: 50%;
		background-color: #fcfcfc;
	}
	75% {
		left: 50%;
		top: 50%;
	}
	100% {
		width: 350px;
		height: 400px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
}

.endAnimateSmall {
	animation: disappearSmall 0.5s ease both;
}

@keyframes disappearSmall {
	0% {
		width: 350px;
		height: 400px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
	25% {
		background-color: #3a9ff5;
		border-radius: 50%;
	}
	50% {
		background-color: #3a9ff5;
	}
	100% {
		background-color: #3a9ff5;
		width: 0;
		height: 0;
		left: 0;
		top: 0;
	}
}
//middle大小
.startAnimateMiddle {
	animation: showDescMiddle 0.5s ease both;
}

@keyframes showDescMiddle {
	0% {
		width: 0;
		height: 0;
		border-radius: 50%;
		background-color: #3a9ff5;
	}
	25% {
		border-radius: 10px;
	}
	50% {
		left: 50%;
		top: 50%;
		background-color: #fcfcfc;
	}
	75% {
		left: 50%;
		top: 50%;
	}
	100% {
		width: 400px;
		height: 450px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
}

.endAnimateMiddle {
	animation: disappearMiddle 0.5s ease both;
}

@keyframes disappearMiddle {
	0% {
		width: 400px;
		height: 450px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
	25% {
		background-color: #3a9ff5;
		border-radius: 50%;
	}
	50% {
		background-color: #3a9ff5;
	}
	100% {
		background-color: #3a9ff5;
		width: 0;
		height: 0;
		left: 0;
		top: 0;
	}
}
//large
.startAnimateLarge {
	animation: showDescLarge 0.3s ease both;
}

@keyframes showDescLarge {
	0% {
		width: 0;
		height: 0;
		border-radius: 50%;
		background-color: #3a9ff5;
	}
	25% {
		border-radius: 10px;
	}
	50% {
		left: 50%;
		top: 50%;
		background-color: #fcfcfc;
	}
	75% {
		left: 50%;
		top: 50%;
	}
	100% {
    width: 800px;
    height: 600px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
}

.endAnimateLarge {
	animation: disappearLarge 0.3s ease both;
}

@keyframes disappearLarge {
  0% {
    width: 800px;
    height: 600px;
    background-color: #ffffff;
    left: 50%;
    top: 50%;
  }
  25% {
    background-color: #3a9ff5;
    border-radius: 50%;
  }
  50% {
    background-color: #3a9ff5;
  }
  100% {
    background-color: #3a9ff5;
    width: 0;
    height: 0;
    left: 0;
    top: 0;
  }
}

//huge
.startAnimateHuge {
	animation: showDescHuge 0.5s ease both;
}

@keyframes showDescHuge {
	0% {
		width: 0;
		height: 0;
		border-radius: 50%;
		background-color: #3a9ff5;
	}
	25% {
		border-radius: 10px;
	}
	50% {
		left: 50%;
		top: 50%;
		background-color: #fcfcfc;
	}
	75% {
		left: 50%;
		top: 50%;
	}
	100% {
		width: 1000px;
		height: 800px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
}

.endAnimateHuge {
	animation: disappearHuge 0.5s ease both;
}

@keyframes disappearHuge {
	0% {
		width: 1000px;
		height: 800px;
		background-color: #ffffff;
		left: 50%;
		top: 50%;
	}
	25% {
		background-color: #3a9ff5;
		border-radius: 50%;
	}
	50% {
		background-color: #3a9ff5;
	}
	100% {
		background-color: #3a9ff5;
		width: 0;
		height: 0;
		left: 0;
		top: 0;
	}
}
</style>