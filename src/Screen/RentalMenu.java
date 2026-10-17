package Screen;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import DVD.DVD;
import Exception.RentalException;
import Guest.Guest;
import Manager.Shop;
import Manager.ZaikoKanri;

public class RentalMenu implements Menu {

	private ZaikoKanri zaiko;
	private Guest guest = null;
	private Shop manager;
	private LocalDate today;

	private LocalDate rentedDay;

	public RentalMenu(ZaikoKanri zaiko, Guest guest, Shop manager, LocalDate today) {
		this.zaiko = zaiko;
		this.guest = guest;
		this.manager = manager;
		this.today = today;
	}

	//★★ここで、貸出処理を変える 違うメソッドのふりをする
	@Override
	public void display(Scanner sc) {

		try {
			guest.checkCanRent();//延滞中なら　例外

			System.out.println("----貸出画面----");
			System.out.println("7日までに返却してください");

			DVD[] dvd = zaiko.getDVD();//ここで、一覧を取得する
			List<DVD> dvdSave = guest.getDvdSave();

			System.out.println("商品一覧です"
					+ "借りたい商品を１～６の数字を入れてください");
			for (int i = 0; i < dvd.length; i++) {
				dvd[i].display();
			}

			DVD dOne; //dOneで一商品取り出している
			try {
				int n = Integer.parseInt(sc.nextLine().trim()) - 1;
				dOne = dvd[n];//範囲外は ArrayIndexOutOfBoundsException
			} catch (NumberFormatException e) {
				System.out.println("半角の数字を入力してください");
				return;
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("1から" + dvd.length + "の番号を入力してください");
				return;
			}

			boolean found = dOne.getRented();

			if (found == false) {
				System.out.println("借りられています、やり直してください");
				return;
			}

			//enum DVDPriceを呼び出す
			manager.cal(dOne.getType(), guest.getResister());

			dOne.rented(today);
			dvdSave.add(dOne);//List<DVD> dvdSaveにどんどんレンタルしていく
		
		} catch (RentalException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public Guest productGuest(List<Guest> list) {
		return null;
	}

}
