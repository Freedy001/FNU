<!--suppress ALL -->
<template>
	<div class="full-screen" @click="$emit('clickOutSide')"
	     :style="{'backgroundColor':`rgba(0, 0, 0,${dynamicOpacity})`,'zIndex':proxy.index}">
		<div @click.stop="" class="stop">
			<slot></slot>
		</div>
	</div>
</template>

<script setup lang="ts">
import {defineEmits, defineProps, getCurrentInstance, onMounted, ref, watch} from "vue";

defineProps(['opacity', 'index', 'opacityImmediately'])
defineEmits(['clickOutSide'])
const {proxy}: any = getCurrentInstance();
//动态透明度
let dynamicOpacity = ref(0)

watch(() => proxy.opacity, () => {
	animate()
})

onMounted(() => {
	const immediately = proxy.opacityImmediately;
	if (immediately) {
			dynamicOpacity.value=immediately;
	} else {
		animate()
	}
})

function animate() {
	const number = (proxy.opacity - dynamicOpacity.value) / 100;
	let interval = setInterval(() => {
		dynamicOpacity.value += number
	}, 5);
	setTimeout(() => {
		clearInterval(interval)
	}, 500)
}


</script>

<style scoped lang="scss">
.full-screen {
	width: 100%;
	height: 100%;
	position: fixed;
	top: 0;
	left: 0;
	z-index: 1000;
	display: flex;
	align-items: center;
	justify-content: center;
}

</style>