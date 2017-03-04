import java.util.Random;

/**
 * WARNING: currently, you cannot set visibility of classes to "public"
 * int the examples. Just write "class Foo {..."
**/
class SatAckermann01 {

	static int ackermann(int m, int n) {
		if (m == 0) {
			return n + 1;
		}
		if (n == 0) {
			return ackermann(m - 1, 1);
		}
		return ackermann(m - 1, ackermann(m, n - 1));
	}

	public static void main(String[] args) {
		Random rand = new Random(42);
		int m = rand.nextInt();
		if (m < 0 || m > 3) {
			return;
		}
		int n = rand.nextInt();
		if (n < 0 || n > 23) {
			return;
		}
		int result = ackermann(m, n);
		if (m < 0 || n < 0 || result >= 0) {
			return;
		} else {
			assert false;
		}
	}
}
