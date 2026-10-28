<template>
  <div class="starfield" aria-hidden="true">
    <div class="stars stars-far"></div>
    <div class="stars stars-mid"></div>
    <div class="stars stars-near"></div>
    <div class="nebula"></div>
  </div>
</template>

<script setup>
// 星星坐标在构建时生成，运行时只有 3 个 div。
// 生成方式：用固定 seed 的伪随机数生成 0-100vw / 0-100vh 坐标，
// 每个坐标写成一条 box-shadow，颜色在白色、灰蓝、淡紫之间随机。
</script>

<style scoped>
.starfield {
  position: absolute;
  inset: -2%;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(ellipse at 50% -18%, var(--space-glow) 0%, transparent 56%),
    radial-gradient(ellipse at 50% 50%, var(--space-mid) 0%, var(--space-deep) 100%);
  animation: drift 90s ease-in-out infinite alternate;
}

.nebula {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(640px 440px at 18% 28%, var(--nebula-primary), transparent 66%),
    radial-gradient(720px 520px at 82% 72%, var(--nebula-secondary), transparent 66%);
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 50%;
}

.stars-far {
  width: 1px;
  height: 1px;
  opacity: 0.55;
  box-shadow: 51.16vw 52.99vh 0px 0 var(--star-blue),
      59.02vw 85.08vh 0px 0 var(--star-blue),
      8.00vw 53.05vh 0px 0 var(--star-blue),
      45.03vw 48.00vh 0px 0 var(--star-violet),
      42.00vw 67.02vh 0px 0 var(--star-blue),
      16.08vw 17.85vh 0px 0 var(--star-white-soft),
      39.59vw 37.70vh 0px 0 var(--star-blue),
      12.75vw 59.71vh 0px 0 var(--star-blue),
      41.84vw 84.43vh 0px 0 var(--star-violet),
      76.88vw 31.76vh 0px 0 var(--star-blue),
      25.17vw 5.62vh 0px 0 var(--star-blue),
      97.77vw 29.68vh 0px 0 var(--star-white),
      74.36vw 34.72vh 0px 0 var(--star-blue),
      57.67vw 84.55vh 0px 0 var(--star-white),
      84.97vw 82.03vh 0px 0 var(--star-white-soft),
      2.54vw 7.67vh 0px 0 var(--star-violet),
      49.73vw 52.61vh 0px 0 var(--star-white-soft),
      50.20vw 73.42vh 0px 0 var(--star-white),
      64.41vw 4.51vh 0px 0 var(--star-white-soft),
      63.48vw 98.44vh 0px 0 var(--star-white-soft),
      46.62vw 35.29vh 0px 0 var(--star-white),
      73.34vw 41.94vh 0px 0 var(--star-violet),
      30.04vw 55.48vh 0px 0 var(--star-white-soft),
      84.24vw 16.02vh 0px 0 var(--star-white),
      63.02vw 62.36vh 0px 0 var(--star-white),
      36.92vw 37.76vh 0px 0 var(--star-violet),
      85.29vw 62.99vh 0px 0 var(--star-white-soft),
      91.88vw 58.72vh 0px 0 var(--star-white-soft),
      30.79vw 51.45vh 0px 0 var(--star-white),
      88.07vw 30.16vh 0px 0 var(--star-white-soft),
      12.35vw 7.98vh 0px 0 var(--star-blue),
      54.45vw 51.41vh 0px 0 var(--star-white),
      55.97vw 62.20vh 0px 0 var(--star-violet),
      96.14vw 59.82vh 0px 0 var(--star-blue),
      57.32vw 48.70vh 0px 0 var(--star-white),
      65.17vw 65.08vh 0px 0 var(--star-violet),
      71.05vw 13.51vh 0px 0 var(--star-white),
      64.69vw 97.21vh 0px 0 var(--star-white-soft),
      92.55vw 37.65vh 0px 0 var(--star-violet),
      65.38vw 7.13vh 0px 0 var(--star-white-soft),
      66.40vw 49.24vh 0px 0 var(--star-blue),
      22.52vw 71.81vh 0px 0 var(--star-white-soft),
      9.20vw 55.39vh 0px 0 var(--star-white-soft),
      18.11vw 81.66vh 0px 0 var(--star-violet),
      75.06vw 0.50vh 0px 0 var(--star-white-soft),
      85.04vw 56.93vh 0px 0 var(--star-blue),
      62.87vw 0.08vh 0px 0 var(--star-white-soft),
      49.12vw 16.31vh 0px 0 var(--star-violet),
      6.37vw 8.48vh 0px 0 var(--star-violet),
      61.28vw 70.39vh 0px 0 var(--star-violet),
      91.86vw 98.24vh 0px 0 var(--star-white),
      16.84vw 59.61vh 0px 0 var(--star-white-soft),
      41.73vw 53.29vh 0px 0 var(--star-white),
      6.09vw 46.26vh 0px 0 var(--star-white-soft),
      41.73vw 72.61vh 0px 0 var(--star-white-soft),
      33.82vw 58.87vh 0px 0 var(--star-white-soft),
      97.53vw 29.69vh 0px 0 var(--star-white-soft),
      24.52vw 68.40vh 0px 0 var(--star-white-soft),
      56.24vw 64.35vh 0px 0 var(--star-white),
      13.07vw 16.48vh 0px 0 var(--star-violet),
      13.57vw 81.69vh 0px 0 var(--star-white),
      63.21vw 82.56vh 0px 0 var(--star-blue),
      81.24vw 9.98vh 0px 0 var(--star-white),
      74.41vw 21.58vh 0px 0 var(--star-white),
      34.27vw 9.71vh 0px 0 var(--star-violet),
      63.90vw 72.07vh 0px 0 var(--star-blue),
      75.64vw 70.67vh 0px 0 var(--star-white-soft),
      76.39vw 22.67vh 0px 0 var(--star-white),
      13.86vw 62.53vh 0px 0 var(--star-white),
      56.36vw 96.59vh 0px 0 var(--star-violet),
      51.98vw 87.61vh 0px 0 var(--star-white),
      52.79vw 76.83vh 0px 0 var(--star-white-soft),
      98.28vw 13.89vh 0px 0 var(--star-violet),
      47.24vw 92.70vh 0px 0 var(--star-violet),
      38.89vw 46.26vh 0px 0 var(--star-white-soft),
      9.88vw 18.77vh 0px 0 var(--star-white),
      8.84vw 93.70vh 0px 0 var(--star-blue),
      57.12vw 23.16vh 0px 0 var(--star-violet),
      7.73vw 83.60vh 0px 0 var(--star-white-soft),
      6.30vw 44.18vh 0px 0 var(--star-white-soft),
      14.01vw 37.21vh 0px 0 var(--star-white),
      89.71vw 57.90vh 0px 0 var(--star-white-soft),
      99.30vw 34.82vh 0px 0 var(--star-blue),
      54.36vw 25.87vh 0px 0 var(--star-blue),
      94.54vw 30.17vh 0px 0 var(--star-white-soft),
      40.69vw 12.74vh 0px 0 var(--star-white),
      63.25vw 2.09vh 0px 0 var(--star-white),
      74.13vw 15.97vh 0px 0 var(--star-white),
      43.20vw 78.29vh 0px 0 var(--star-blue),
      60.53vw 67.54vh 0px 0 var(--star-white-soft);
  animation: twinkle-far 8s ease-in-out infinite alternate;
}

.stars-mid {
  width: 1.5px;
  height: 1.5px;
  opacity: 0.7;
  box-shadow: 62.19vw 23.67vh 0.5px 0 var(--star-white-mid),
      20.53vw 36.46vh 0.5px 0 var(--star-violet-mid),
      44.54vw 67.32vh 0.5px 0 var(--star-violet-mid),
      1.65vw 10.73vh 0.5px 0 var(--star-white-mid),
      18.29vw 49.78vh 0.5px 0 var(--star-white-mid),
      55.21vw 22.44vh 0.5px 0 var(--star-blue-mid),
      78.14vw 66.57vh 0.5px 0 var(--star-violet-mid),
      10.21vw 66.36vh 0.5px 0 var(--star-blue-mid),
      0.46vw 4.76vh 0.5px 0 var(--star-blue-mid),
      46.40vw 17.15vh 0.5px 0 var(--star-white-mid),
      81.63vw 11.01vh 0.5px 0 var(--star-white-mid),
      4.52vw 29.33vh 0.5px 0 var(--star-white-mid),
      12.79vw 32.36vh 0.5px 0 var(--star-blue-mid),
      12.14vw 0.28vh 0.5px 0 var(--star-blue-mid),
      5.38vw 79.67vh 0.5px 0 var(--star-blue-mid),
      3.75vw 11.85vh 0.5px 0 var(--star-blue-mid),
      26.11vw 82.64vh 0.5px 0 var(--star-white-mid),
      41.68vw 66.98vh 0.5px 0 var(--star-blue-mid),
      89.74vw 41.94vh 0.5px 0 var(--star-white-mid),
      27.39vw 42.85vh 0.5px 0 var(--star-violet-mid),
      52.41vw 60.47vh 0.5px 0 var(--star-violet-mid),
      12.17vw 35.07vh 0.5px 0 var(--star-violet-mid),
      11.01vw 4.98vh 0.5px 0 var(--star-blue-mid),
      78.72vw 47.94vh 0.5px 0 var(--star-violet-mid),
      39.93vw 12.19vh 0.5px 0 var(--star-white-mid),
      61.51vw 55.98vh 0.5px 0 var(--star-blue-mid),
      39.42vw 29.63vh 0.5px 0 var(--star-white-mid),
      82.98vw 17.71vh 0.5px 0 var(--star-blue-mid),
      14.45vw 95.52vh 0.5px 0 var(--star-blue-mid),
      56.31vw 89.42vh 0.5px 0 var(--star-white-mid),
      70.70vw 40.55vh 0.5px 0 var(--star-blue-mid),
      24.08vw 91.03vh 0.5px 0 var(--star-blue-mid),
      99.42vw 53.44vh 0.5px 0 var(--star-blue-mid),
      66.50vw 8.66vh 0.5px 0 var(--star-blue-mid),
      3.95vw 59.77vh 0.5px 0 var(--star-white-mid),
      48.67vw 56.40vh 0.5px 0 var(--star-violet-mid),
      49.56vw 46.46vh 0.5px 0 var(--star-white-mid),
      0.90vw 45.58vh 0.5px 0 var(--star-white-mid),
      60.31vw 42.81vh 0.5px 0 var(--star-violet-mid),
      61.57vw 73.81vh 0.5px 0 var(--star-blue-mid),
      99.76vw 42.49vh 0.5px 0 var(--star-violet-mid),
      8.18vw 32.96vh 0.5px 0 var(--star-blue-mid),
      52.46vw 71.10vh 0.5px 0 var(--star-violet-mid),
      66.11vw 70.76vh 0.5px 0 var(--star-blue-mid),
      48.51vw 27.71vh 0.5px 0 var(--star-blue-mid),
      48.92vw 75.97vh 0.5px 0 var(--star-violet-mid),
      32.71vw 62.55vh 0.5px 0 var(--star-blue-mid),
      84.22vw 71.62vh 0.5px 0 var(--star-violet-mid),
      90.22vw 31.13vh 0.5px 0 var(--star-violet-mid),
      28.24vw 62.62vh 0.5px 0 var(--star-white-mid),
      91.89vw 11.36vh 0.5px 0 var(--star-white-mid),
      80.34vw 33.21vh 0.5px 0 var(--star-violet-mid),
      60.27vw 7.82vh 0.5px 0 var(--star-white-mid),
      32.28vw 12.29vh 0.5px 0 var(--star-white-mid),
      53.97vw 77.60vh 0.5px 0 var(--star-white-mid);
  animation: twinkle-mid 12s ease-in-out infinite alternate;
  animation-delay: -4s;
}

.stars-near {
  width: 2px;
  height: 2px;
  opacity: 0.9;
  box-shadow: 55.08vw 73.95vh 1.2px 0 var(--star-blue-near),
      1.64vw 44.65vh 1.2px 0 var(--star-violet-near),
      26.60vw 59.21vh 1.2px 0 var(--star-violet-near),
      96.27vw 32.24vh 1.2px 0 var(--star-violet-near),
      22.69vw 81.94vh 1.2px 0 var(--star-violet-near),
      62.83vw 57.00vh 1.2px 0 var(--star-white-near),
      8.06vw 27.47vh 1.2px 0 var(--star-violet-near),
      23.09vw 73.02vh 1.2px 0 var(--star-blue-near),
      55.62vw 62.16vh 1.2px 0 var(--star-violet-near),
      67.24vw 42.15vh 1.2px 0 var(--star-violet-near),
      87.72vw 69.88vh 1.2px 0 var(--star-blue-near),
      89.73vw 84.76vh 1.2px 0 var(--star-blue-near),
      57.13vw 12.96vh 1.2px 0 var(--star-blue-near),
      23.39vw 51.80vh 1.2px 0 var(--star-violet-near),
      15.46vw 91.27vh 1.2px 0 var(--star-violet-near),
      91.13vw 60.91vh 1.2px 0 var(--star-violet-near),
      38.69vw 28.41vh 1.2px 0 var(--star-white-near),
      37.28vw 7.14vh 1.2px 0 var(--star-blue-near),
      49.70vw 27.56vh 1.2px 0 var(--star-blue-near),
      78.72vw 74.62vh 1.2px 0 var(--star-violet-near),
      75.01vw 77.71vh 1.2px 0 var(--star-violet-near),
      99.64vw 5.89vh 1.2px 0 var(--star-blue-near),
      73.20vw 98.77vh 1.2px 0 var(--star-violet-near),
      7.89vw 4.88vh 1.2px 0 var(--star-white-near),
      79.05vw 84.27vh 1.2px 0 var(--star-violet-near),
      74.30vw 36.27vh 1.2px 0 var(--star-violet-near),
      83.19vw 98.97vh 1.2px 0 var(--star-white-near),
      47.70vw 6.18vh 1.2px 0 var(--star-blue-near);
  animation: twinkle-near 16s ease-in-out infinite alternate;
  animation-delay: -7s;
}

@keyframes twinkle-far {
  from { opacity: 0.3; }
  to { opacity: 0.7; }
}

@keyframes twinkle-mid {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

@keyframes twinkle-near {
  0%, 100% { opacity: 0.65; }
  50% { opacity: 1; }
}

@keyframes drift {
  from { transform: translate3d(0, 0, 0); }
  to { transform: translate3d(0, -1.8%, 0); }
}

@media (prefers-reduced-motion: reduce) {
  .starfield,
  .stars {
    animation: none;
  }
}
</style>
