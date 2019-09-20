import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

/**
 * 问题1 间隔时间intervalSec不能太长，否则longAdder超出
 * 问题2 如果间隔时间（intervalSec）过短，有可能超出预留桶
 * @Author yuchen.xiao
 * @Date: 2019-09-18 16:51
 * @Version 1.0
 */
public class Qps {
	private final MyNode[] myNodes;
	//间隔毫秒
	private final int intervalSec;
	private final int qpsNum;

	public Qps(){
		//90天，1小时统计一次，
		this(2161,3600);
	}
	public Qps(int qpsNum, int intervalSec) {
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
		private volatile long lastTime ;
		private volatile long qps ;
		public MyNode() {
			longAdder = new LongAdder();
			lastTime = System.currentTimeMillis();
			qps = 0;
		}
		public void add(long now,long minTime,int intervalSec){
			//判断是否清空
			if (lastTime<minTime){
				reset(now);
			}
			//添加
			longAdder.increment();
			lastTime = now;
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
		Qps qps = new Qps(10,2);
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
