import java.util.ArrayList;

/**
 * @Author yuchen.xiao
 * @Date: 2019-10-14 17:36
 * @Version 1.0
 */
public class mian {
	public static void main(String[] args) {
		System.out.println(reverseBits(43261596));
	}
	public static int reverseBits(int n) {
		ArrayList<Integer> list = new ArrayList<>();
		while (n!=0){

			list.add(n%2);
			n = n >>> 1;

		}
		int  reault =0;
		for (Integer a:list){
			reault = reault << 1 +a;
		}
		return reault;

	}
}
