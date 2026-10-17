package Screen;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import Exception.SystemMenuException;
import Guest.Guest;
import Manager.Employee;
import Manager.Shop;
import Manager.ZaikoKanri;

public class TopMenu implements Menu {

	private ZaikoKanri zaiko;
	private Scanner sc;
	private Shop ai;
	private LocalDate today = LocalDate.now();

	public TopMenu(ZaikoKanri zaikoKanri, Scanner sc, Shop manager) {
		this.zaiko = zaikoKanri;
		this.sc = sc;
		this.ai = manager;
	}

	@Override
	public void display(Scanner sc) {

		boolean found = true;

		//このリストを通常、異常客として使い分けたい
		List<Guest> list = new ArrayList<>();

		Guest guest = productGuest(list);

		while (found) {

			try {

				System.out.println("今日は----" + today + "");

				String text = """
						レンタルDVD
						1,レンタル
						2,返却
						3,検索
						4,会員登録
						5,現在の状態を確認する

						6,次の客を生成する OR 以前の客を呼び出す
						7,1日目が終了する
						8,店の関係者のみ閲覧可能
						9,システム自体を止める
						""";

				System.out.println(text);

				//入力はすべて nextLine() で1行ずつ読む（next()やnextInt()と混ぜると改行が残って壊れる）
				//数字以外だと NumberFormatException になり、下の catch でメニューに戻る
				int select = Integer.parseInt(sc.nextLine().trim());

				//続き
				switch (select) {
				case 1 -> dispSubMenu(new RentalMenu(zaiko, guest, ai, today));
				case 2 -> dispSubMenu(new ReturnMenu(guest, ai));
				case 3 -> dispSubMenu(new Search(guest, zaiko));
				case 4 -> dispSubMenu(new Register(guest, ai));
				case 5 -> dispSubMenu(new Check(guest, today));
				case 6 -> guest = productGuest(list);

				case 7 -> {
					dispSubMenu(new EndDay(guest, today));
					today = today.plusDays(1);
				}

				case 8 -> {

					System.out.println("関係者ですか?"
							+ "名前と　ID番号を入力してください");

					checkStaff(list);

				}

				case 9 -> found = false;

				default -> {
					System.out.println("例外が発生します");
					throw new SystemMenuException("数値を入力してください");
				}

				}
				
			} catch (SystemMenuException e) {
				System.out.println(e.getMessage());
			} catch (NumberFormatException e) {
				System.out.println("半角の数字を入力してください");
			}
		}
		System.out.println("終了しました");

	}

	//例外はそのまま display の catch に渡して、トップメニューに戻る
	void checkStaff(List<Guest> list) throws SystemMenuException {
		System.out.println("名前を入力してください");
		String name = sc.nextLine().trim();
		if (name.isEmpty()) {
			throw new SystemMenuException("名前が空です");
		}

		System.out.println("ID番号を入力してください");
		int id = Integer.parseInt(sc.nextLine().trim());//数字以外は NumberFormatException

		//AiManagerが社員のリストをもう、もっているので　
		//ポリモーフィズム？
		List<Employee> listEmployee = ai.getListEmployee();

		for (Employee man : listEmployee) {
			if (man.getName().equals(name) && man.getId() == id) {
				System.out.println("関係者");
				man.printCheck(list);
			}
		}

	}

	//処理を変えていく
	void dispSubMenu(Menu menu) {
		menu.display(sc);
	}

	@Override
	public Guest productGuest(List<Guest> list) {

		while (true) {
			try {

				System.out.println("名前を入力してください");

				String name = sc.nextLine().trim();
				if (name.isEmpty() || name.length() >= 20 || name.matches("[0-9]+")) {
					throw new SystemMenuException("名前は数字だけ・空・20文字以上にはできません");
				}

				Guest guest;
				for (Guest g : list) {
					if (g.getName().equals(name)) {
						System.out.println("おかえりなさい" + g.getName() + "様");
						return g;
					}
				}

				//始めは通常客で生成する
				guest = new Guest(name);
				list.add(guest);
				return guest;
			} catch (SystemMenuException e) {
				System.out.println(e.getMessage());
			}

		}
	}
}
