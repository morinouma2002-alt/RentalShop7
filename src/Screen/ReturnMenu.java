package Screen;

import java.util.List;
import java.util.Scanner;

import DVD.DVD;
import Guest.Guest;
import Guest.GuestStatus;
import Manager.Shop;

public class ReturnMenu implements Menu {

	private Guest guest;
	private Shop manager;

	public ReturnMenu(Guest guest, Shop manager) {
		this.guest = guest;
		this.manager = manager;
	}

	@Override
	public void display(Scanner sc) {
		List<DVD> dvdSave = guest.getDvdSave();

		if (dvdSave.size() == 0) {
			System.out.println("返却する必要はないです");
			return;
		}

		System.out.println("あなたが、現在借りている作品は");
		for (int i = 0; i < dvdSave.size(); i++) {
			DVD d = dvdSave.get(i);
			System.out.println((i + 1) + "番目;" + d.getName());
		}

		System.out.println("返却するのを番号で選んでください");
		DVD d;
		try {
			int n = Integer.parseInt(sc.nextLine().trim()) - 1;
			d = dvdSave.remove(n);//範囲外は IndexOutOfBoundsException（このとき何も削除されない）
		} catch (NumberFormatException e) {
			System.out.println("半角の数字を入力してください");
			return;
		} catch (IndexOutOfBoundsException e) {
			System.out.println("1から" + dvdSave.size() + "の番号を入力してください");
			return;
		}
		d.returnRented();

		// 延滞客が、全部返し終わったときだけ通常客に戻す
		if (guest.getStatus() == GuestStatus.POSTPONE && dvdSave.isEmpty()) {
			System.out.println("延滞料として500円徴収します");
			manager.postAssets();

			guest.clearPostpone();//同じ客の状態を戻すだけ
			System.out.println("通常客に戻ります");
		}
	}

	@Override
	public Guest productGuest(List<Guest> list) {
		return null;
	}
}
