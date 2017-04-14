package ads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import ads.FS;

public class Main {
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("welcome to TFS demo");
		Scanner scanner = new Scanner(System.in);
		Scanner scanner2 = new Scanner(new FileInputStream(new File("data1.txt")));
		String string1 = "sdasdasdasdasdasdasdasdasd";
		String string = "";
		while (true) {
			try {
				string += scanner2.nextLine() + "\r\n";
			} catch (Exception e) {
				scanner2.close();
				break;
			}
		}

		FS fs = new FS();
		byte[] bs = string.getBytes();
		byte[] bs2 = string1.getBytes();
		while (true) {
			String command = scanner.nextLine();
			if (command.equals("quit")) {
				break;
			}
			String functionName = command.substring(0, command.indexOf("(")).trim();
			String para1 = new String();
			String para = new String();
			if (command.indexOf(",") != -1) {
				para = command.substring(command.indexOf("(") + 1, command.indexOf(","));
			} else
				para = command.substring(command.indexOf("(") + 1, command.indexOf(")"));
			switch (functionName) {
			case "open":
				fs.open(para);
				break;
			case "openTrans":
				fs.openTrans(para);
				break;
			case "write":
				fs.write(para, bs2);
				break;
			case "writeTrans":
				fs.writeTrans(para, bs);
				break;
			case "read":
				fs.read(para);
				break;
			case "list":
				fs.list(para);
				break;
			case "close":
				fs.close(para);
				break;
			case "closeTrans":
				fs.closeTrans(para);
				break;
			case "delete":
				fs.delete(para);
				break;
			case "deleteTrans":
				fs.deleteTrans(para);
				break;
			default:
				break;
			}
		}
		scanner.close();
	}
}
