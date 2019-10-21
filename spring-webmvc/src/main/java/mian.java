import java.util.ArrayList;

/**
 * @Author yuchen.xiao
 * @Date: 2019-10-14 17:36
 * @Version 1.0
 */
public class mian {
	public static void main(String[] args) {
		System.out.println(isPowerOfTwo(-2147483648));
	}
	public static boolean isPowerOfTwo(int n) {
		int result = 0;
		if ((n>>31 &1)==1){
			return false;
		}
		for(int index=0;index<31;index++){
			int a = n>>index;
			int oneNum= a&1;
			if(oneNum==1){
				result++;
			}
			if(result==2){
				return false;
			}
		}
		return result == 1;
	}
}
