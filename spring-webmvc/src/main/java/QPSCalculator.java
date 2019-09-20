//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.LongAdder;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * @Author yuchen.xiao
// * @Date: 2019-09-19 09:32
// * @Version 1.0
// */
//public class QPSCalculator {
//	private RollingNumber rollingNumber;
//
//	public QPSCalculator() {
//		this.rollingNumber = new RollingNumber();
//	}
//
//
//	public void pass() {
//		rollingNumber.record();
//	}
//
//
//	private static final class RollingNumber {
//		/**
//		 * 槽位的数量
//		 */
//		private int sizeOfBuckets;
//		/**
//		 * 时间片，单位毫秒
//		 */
//		private int unitOfTimeSlice;
//		/**
//		 * 用于判断是否可跳过锁争抢
//		 */
//		private int timeSliceUsedToCheckIfPossibleToBypass;
//		/**
//		 * 槽位
//		 */
//		private Bucket[] buckets;
//		/**
//		 * 目标槽位的位置
//		 */
//		private volatile Integer targetBucketPosition;
//		/**
//		 * 接近目标槽位最新更新时间的时间
//		 */
//		private volatile Long latestPassedTimeCloseToTargetBucket;
//		/**
//		 * 进入下一个槽位时使用的锁
//		 */
//		private ReentrantLock enterNextBucketLock;
//		/**
//		 * 默认60个槽位，槽位的时间片为1000毫秒
//		 */
//		public RollingNumber() {
//			this(60, 1000);
//		}
//		/**
//		 * 初始化Bucket数量与每个Bucket的时间片等
//		 *
//		 * @param sizeOfBuckets
//		 * @param unitOfTimeSlice
//		 */
//		public RollingNumber(int sizeOfBuckets, int unitOfTimeSlice) {
//			this.latestPassedTimeCloseToTargetBucket = System.currentTimeMillis() - (2 * unitOfTimeSlice);
//			this.targetBucketPosition = null;
//			this.sizeOfBuckets = sizeOfBuckets;
//			this.unitOfTimeSlice = unitOfTimeSlice;
//			this.enterNextBucketLock = new ReentrantLock();
//			this.buckets = new Bucket[sizeOfBuckets];
//			this.timeSliceUsedToCheckIfPossibleToBypass = 3 * unitOfTimeSlice;
//			for (int i = 0; i < sizeOfBuckets; i++) {
//				this.buckets[i] = new Bucket();
//			}
//		}
//
//
//		private void record() {
//			long nowTime = System.currentTimeMillis();
//			if (targetBucketPosition == null) {
//				targetBucketPosition = (int) (nowTime / unitOfTimeSlice) % sizeOfBuckets;
//			}
//			Bucket currentBucket = buckets[targetBucketPosition];
//			//最后更新在一个单位时间内，跳过，直接更新。问题 ，可能并不在一个桶里
//			if (nowTime - latestPassedTimeCloseToTargetBucket >= unitOfTimeSlice) {
//				//加锁，且最后一次更新的时间距离现在少于三个时间单位，跳过，直接更新。
//				if (enterNextBucketLock.isLocked() && (nowTime - latestPassedTimeCloseToTargetBucket) < timeSliceUsedToCheckIfPossibleToBypass) {
//				} else {
//					//不是以上两种方式，都要进入下边逻辑
//					try {
//						enterNextBucketLock.lock();
//						//加锁后再次判断
//						if (nowTime - latestPassedTimeCloseToTargetBucket >= unitOfTimeSlice) {
//							int nextTargetBucketPosition = (int) (nowTime / unitOfTimeSlice) % sizeOfBuckets;
//							Bucket nextBucket = buckets[nextTargetBucketPosition];
//							if (nextBucket.equals(currentBucket)) {
//								//永远成立的if语句，傻叉判定
//								if (nowTime - latestPassedTimeCloseToTargetBucket >= unitOfTimeSlice) {
//									latestPassedTimeCloseToTargetBucket = nowTime;
//								}
//							} else {
//								nextBucket.reset(nowTime);
//								targetBucketPosition = nextTargetBucketPosition;
//								latestPassedTimeCloseToTargetBucket = nowTime;
//							}
//							nextBucket.pass();
//							return;
//						} else {
//							//永远到不了的分支
//							currentBucket = buckets[targetBucketPosition];//毫无作用
//						}
//					} finally {
//						enterNextBucketLock.unlock();
//					}
//				}
//			}
//			currentBucket.pass();
//		}
//
//		public Bucket[] getBuckets() {
//			return buckets;
//		}
//	}
//
//
//	private static class Bucket implements Serializable {
//
//		private static final long serialVersionUID = -9085720164508215774L;
//
//		private Long latestPassedTime;
//
//		private LongAdder longAdder;
//
//		public Bucket() {
//			this.latestPassedTime = System.currentTimeMillis();
//			this.longAdder = new LongAdder();
//		}
//
//
//		public void pass() {
//			longAdder.add(1);
//		}
//
//		public long countTotalPassed() {
//			return longAdder.sum();
//		}
//
//		public long getLatestPassedTime() {
//			return latestPassedTime;
//		}
//
//		public void reset(long latestPassedTime) {
//			this.longAdder.reset();
//			this.latestPassedTime = latestPassedTime;
//		}
//	}
//
//
//
//	public static void main(String[] args) {
//		try {
//			final QPSCalculator qpsCalculator = new QPSCalculator();
//			int threadNum = 4;
//			CountDownLatch countDownLatch = new CountDownLatch(threadNum);
//			List<Thread> threadList = new ArrayList<Thread>();
//			for (int i = 0; i < threadNum; i++) {
//				threadList.add(new Thread() {
//					public void run() {
//						for (int i = 0; i < 50000000; i++) {
//							qpsCalculator.pass();
//						}
//						countDownLatch.countDown();
//					}
//				});
//			}
//
//			long startTime = System.currentTimeMillis();
//			for (Thread thread : threadList) {
//				thread.start();
//			}
//			countDownLatch.await();
//			long endTime = System.currentTimeMillis();
//			long totalTime = endTime - startTime;
//			System.out.print("totalMilliseconds:  " + totalTime);
//			TimeUnit.SECONDS.sleep(1000L);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//}
