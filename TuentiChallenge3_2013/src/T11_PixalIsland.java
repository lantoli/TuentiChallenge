import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.Decoder;

public class T11_PixalIsland {

	/*
	 * NOTE: We'll be using a structure very similar to array-based heaps. First element has the root, next 4 elements
	 * has root's children, next 16 elements has level-1's children and so on multiplying by 4 in each level. More info
	 * here (in the problem is 4 elements instead of 2 but everything applies) :
	 * http://en.wikipedia.org/wiki/Binary_heap
	 * 
	 * Using zxing-2.1 library for QR codes
	 */

	// Set FILENAME = null for standard input/output
	final static String FILENAME = null;
	// final static String FILENAME = "sample_pixel";

	final static String FILENAME_IN = FILENAME + ".in";
	final static String FILENAME_OUT = FILENAME + ".out";

	final static byte NODE = 'p';
	final static byte WHITE = 'w';
	final static byte BLACK = 'b';

	final static int NUM_CHILDREN = 4;

	public static void main(String[] args) throws Exception {
		if (FILENAME != null) {
			System.setIn(new FileInputStream(FILENAME_IN));
			System.setOut(new PrintStream(new FileOutputStream(FILENAME_OUT)));
		}

		try (Scanner in = new Scanner(System.in)) {
			int tests = in.nextInt();
			in.nextLine();
			for (int test = 0; test < tests; test++) {
				byte[] square = processPixels(in.nextLine().split("\\s+"));
				boolean[][] board = getBoard(square);
				String ret = getQRCode(board);
				System.out.println(ret);
			}
		}
	}

	private static byte[] processPixels(String[] strs) {
		byte[][] squares = new byte[strs.length][];
		for (int i = 0; i < strs.length; i++) {
			squares[i] = getSquareFromStr(strs[i]);
		}
		byte[] sum = getSum(squares);
		fillSquareGapsTowardsParents(sum);
		return sum;
	}

	private static byte[] getSquareFromStr(String str) {
		int levels = getLevels(str);
		int elementsInLevel = 1;
		int size = 0;
		for (int i = 0; i <= levels; i++) {
			size += elementsInLevel;
			elementsInLevel *= NUM_CHILDREN;
		}
		byte[] ret = new byte[size];
		ret[0] = (byte) str.charAt(0); // Root node

		List<Integer> children = new ArrayList<>();
		if (ret[0] == NODE) {
			children.add(0);
		}
		int pos = 1;
		while (children.size() > 0) {
			List<Integer> newChildren = new ArrayList<>();
			for (int child : children) {
				for (int i = 1; i <= NUM_CHILDREN; i++) {
					byte elm = (byte) str.charAt(pos++);
					int posChild = child * NUM_CHILDREN + i;
					ret[posChild] = elm;
					if (elm == NODE) {
						newChildren.add(posChild);
					}
				}
			}
			children = newChildren;
		}
		assert pos == str.length(); // Should have read all the string
		fillSquareGapsTowardsChildren(ret);
		return ret;
	}

	/**
	 * Fill empty children squares with their parent's values
	 */
	private static void fillSquareGapsTowardsChildren(byte[] square) {
		for (int i = 1; i < square.length; i++) {
			if (square[i] == 0) {
				square[i] = square[(i - 1) / NUM_CHILDREN];
			}
		}
	}

	/**
	 * Propagate non-node values to their children
	 */
	private static void fillSquareGapsTowardsParents(byte[] square) {
		int len = square.length;
		int pos = getPosLastLevel(square);
		int parents = (len - pos + 1) / NUM_CHILDREN;
		while (parents > 0) {
			for (int i = 0; i < parents; i++) {
				int posFirstChild = pos + i * NUM_CHILDREN;
				int posParent = (posFirstChild - 1) / NUM_CHILDREN;

				if (allSameType(square, posFirstChild, BLACK)) {
					square[posParent] = BLACK;
				} else if (allSameType(square, posFirstChild, WHITE)) {
					square[posParent] = WHITE;
				} else {
					square[posParent] = NODE;
				}
			}
			pos -= parents;
			parents /= NUM_CHILDREN;
		}
	}

	private static boolean allSameType(byte[] square, int posFirstChild, byte type) {
		for (int i = 0; i < NUM_CHILDREN; i++)
			if (square[posFirstChild + i] != type)
				return false;
		return true;
	}

	private static int getLevels(String str) {
		int level = -1;
		int pos = 0;
		int childNodes = 1;
		int nodesInNextLevel = 1;
		while (childNodes > 0) {
			childNodes = 0;
			for (int i = 0; i < nodesInNextLevel; i++) {
				if (str.charAt(pos + i) == NODE) {
					childNodes++;
				}
			}
			level++;
			pos += nodesInNextLevel;
			nodesInNextLevel = NUM_CHILDREN * childNodes;
		}
		return level;
	}

	private static byte[] getSum(byte[][] squares) {
		int numSquares = squares.length;
		int size = squares[0].length;
		byte[] ret = new byte[size];
		for (int i = getPosLastLevel(squares[0]); i < size; i++) {
			boolean foundBlack = false;
			for (int n = 0; n < numSquares; n++) {
				if (squares[n][i] == BLACK) {
					foundBlack = true;
					break;
				}
			}
			ret[i] = foundBlack ? BLACK : WHITE;
		}

		return ret;
	}

	private static int getPosLastLevel(byte[] square) {
		int size = square.length;
		int levelSize = 1;
		int postLastLevel = 0;
		while (size > levelSize) {
			size -= levelSize;
			postLastLevel += levelSize;
			levelSize *= NUM_CHILDREN;
		}
		return postLastLevel;
	}

	private static byte[] getSquareLastLevelOnly(byte[] square) {
		return Arrays.copyOfRange(square, getPosLastLevel(square), square.length);
	}

	private static boolean[][] getBoard(byte[] squares) {
		byte[] squareLastLevel = getSquareLastLevelOnly(squares);
		return getBoard(squareLastLevel, 0, squareLastLevel.length);
	}

	private static boolean[][] getBoard(byte[] square, int posIni, int posEnd) {
		int len = posEnd - posIni + 1;
		int dim = (int) Math.sqrt(len);
		boolean[][] ret = new boolean[dim][dim];
		if (dim == 1) {
			ret[0][0] = square[posIni] == BLACK;
		} else {
			int dimBuckets = dim / 2;
			int lenBuckets = len / 4;

			boolean[][][] buckets = new boolean[4][][];
			for (int i = 0; i < 4; i++) {
				buckets[i] = getBoard(square, posIni + lenBuckets * i, posIni + lenBuckets * (i + 1));
			}

			// To get the spiral
			int[] yori = { 0, 0, 1, 1 };
			int[] xori = { 1, 0, 0, 1 };
			for (int i = 0; i < 4; i++) {
				for (int y = 0; y < dimBuckets; y++) {
					for (int x = 0; x < dimBuckets; x++) {
						ret[yori[i] * dimBuckets + y][xori[i] * dimBuckets + x] = buckets[i][y][x];
					}
				}
			}

		}
		return ret;
	}

	final private static int QR_SIZE = 29;

	private static String getQRCode(boolean[][] board) {
		try {
			BitMatrix matrix = new BitMatrix(QR_SIZE, QR_SIZE);
			for (int y = 0; y < QR_SIZE; y++) {
				for (int x = 0; x < QR_SIZE; x++) {
					if (board[y][x]) {
						matrix.set(x, y);
					}
				}
			}
			String text = new Decoder().decode(matrix).getText();
			StringBuilder str = new StringBuilder();
			for (char ch : text.toCharArray())
				if ((int) ch > 0 && (int) ch < 256) {
					str.append(ch);
				}
			return str.toString().trim();
		} catch (Exception e) {
			return "NO QR CODE FOUND";
		}
	}
}