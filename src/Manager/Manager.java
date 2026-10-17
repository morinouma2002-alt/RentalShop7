package Manager;

import java.util.List;
import java.util.Scanner;

import DVD.DVD;
import Guest.Guest;
import Guest.GuestStatus;

public class Manager extends Employee {

	private ZaikoKanri zaiko;

	public Manager(String name, int id, ZaikoKanri zaiko, Scanner sc, Shop shop) {
		super(name, id, sc, shop);
		this.zaiko = zaiko;
	}

	@Override
	public void printCheck(List<Guest> list) {

		System.out.println("----店長----");
		boolean found = true;

		while (found) {

			String textBlock = """
					1,在庫チェック
					2,売上をチェックする
					3,延滞客を見る
					4,以上は終了
					""";

			System.out.println(textBlock);
			try {
				int n = Integer.parseInt(sc.nextLine().trim());

				switch (n) {
				case 1 -> checkZaiko();
				case 2 -> shop.checkAssets();

				case 3 -> {
					System.out.println("延滞客をチェックする");
					printCheckEntai(list);
				}

				case 4 -> found = false;
				default -> System.out.println("1から4選んでださい");
				}
			} catch (NumberFormatException e) {
				System.out.println("半角の数字を入力してください");
			}

		}
	}

	private void checkZaiko() {
		DVD[] dvd = zaiko.getDVD();
		for (DVD d : dvd) {
			d.display();
		}
	}

	private void printCheckEntai(List<Guest> list) {
		if (list.size() == 0) {
			System.out.println("客はいない");
			return;
		}

		boolean found = false;//延滞客が見つかったか
		System.out.println("延滞客は");

		for (int i = 0; i < list.size(); i++) {
			Guest guest = list.get(i);

			if (guest.getStatus() == GuestStatus.POSTPONE) {
				found = true;
				System.out.println(guest.getName() + "さん");
			}
		}

		if (!found) {
			System.out.println("当てはまる人は、居ませんでした");
		}
	}
}
