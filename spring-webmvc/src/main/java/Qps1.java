import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

/**
 * 问题1 间隔时间intervalSec不能太长，否则longAdder超出
 * @Author yuchen.xiao
 * @Date: 2019-09-18 16:51
 * @Version 1.0
 */
public class Qps1 {
	private final MyNode[] myNodes;
	//间隔毫秒
	private final int intervalSec;
	private final int qpsNum;

	public Qps1(){
		//90天，1小时统计一次，
		this(2160,3600);
	}
	public Qps1(int qpsNum, int intervalSec) {
		this.qpsNum = qpsNum;
		this.myNodes = new MyNode[qpsNum+2];
		for (int a=0;a<qpsNum+2;a++){
			myNodes[a] = new MyNode();
		}
		this.intervalSec = intervalSec;
	}
	public void add(){
		//获取当前时间
		long now = System.currentTimeMillis();
		//找到当前桶
		int nodeNum = queryPresentNodeNum(now);
		//当前桶累加计算
		myNodes[nodeNum].add(now,queryPresentLeastTime(now),intervalSec);
	}

	public Long[] queryQps(){
		Long[] result = new Long[qpsNum];
		long now = System.currentTimeMillis();
		int nowNum = queryPresentNodeNum(now);

		for (int index = 0;index<qpsNum;index++){
			int myNodeIndex = index + nowNum + 2;
			myNodeIndex = myNodeIndex>=myNodes.length?myNodeIndex-myNodes.length:myNodeIndex;
			result[index] = myNodes[myNodeIndex].getQps(queryPresentLeastTime(now));
		}

		return result;
	}

	private int queryPresentNodeNum(long now){
		return (int) ((now / (intervalSec*1000)) % myNodes.length);
	}

	private Long queryPresentLeastTime(long now){
		return ((now / (intervalSec*1000)) * intervalSec*1000)-((myNodes.length-1)*intervalSec*1000);
	}

	public class MyNode{
		private final LongAdder longAdder;
		//同一周期中 最考后的一次
		private volatile long lastTime ;
		private volatile long qps ;

		public MyNode() {
			longAdder = new LongAdder();
			lastTime = System.currentTimeMillis();
			qps = 0;
		}
		public void add(long now,long minTime,int intervalSec){
			//如果有早的请求 ，不更新时间，只加数量计算qps
			//如果有特别早的请求存在 依然会造成数据混乱，但我认为这种情况不可能发生
			if (now<lastTime){
				longAdder.increment();
				qps = longAdder.longValue()/intervalSec;
				return;
			}
			//判断是否清空
			//加上了锁，防止重复清空，lastTime时间混乱等问题
			synchronized (this){
				if (lastTime<minTime){
					reset(now);
				}
				if (now>lastTime){
					lastTime = now;
				}
			}
			//添加
			longAdder.increment();
			qps = longAdder.longValue()/intervalSec;
		}
		private void reset(long now){
			longAdder.reset();
			qps = 0;
			lastTime = now;
		}
		public long getQps(long leastTime) {
			//判断是否是历史遗留数据
			if (lastTime<leastTime){
				return 0;
			}else {
				return qps;
			}

		}
	}

	public static void main(String[] args) {
//		long now = System.currentTimeMillis();
//		long aa = now/5000;
//		long aa1 = aa*5000;
//		long res = aa1-5000*4;
		Qps1 qps = new Qps1(10,2);
		new Thread(()->{

			while (true){
				try {
					Thread.sleep(1000);
				}catch (Exception e){
					System.out.println(e);
				}
				System.out.println(Arrays.toString(qps.queryQps()));
			}
		}).start();
		new Thread(()->{
			while (true){
				try {
					Thread.sleep(10);
				}catch (Exception e){
					System.out.println(e);
				}
				qps.add();
			}

		}).start();
		while (true){
			try {
				Thread.sleep(50);
			}catch (Exception e){
				System.out.println(e);
			}
			qps.add();
		}
	}
}
