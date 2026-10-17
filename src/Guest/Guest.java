package Guest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import DVD.DVD;
import Exception.RentalException;

public class Guest {

	public Scanner sc = new Scanner(System.in);

	protected String name;

	private Guest postGuest;//延滞客になったら使う

	//始めは、未会員登録
	protected boolean register = false;

	//客の状態はこの1つだけで管理する。始めは通常客
	private GuestStatus status = GuestStatus.NORMAL;

	//guestが借りた、商品を貯める
	protected List<DVD> dvdSave = new ArrayList<>();

	public Guest(String name) {
		this.name = name;
	}

	public List<DVD> getDvdSave() {
		return dvdSave;
	}

	public void checkCanRent() throws RentalException{
		if(status != GuestStatus.NORMAL) {
			throw new RentalException("延滞中なので、借りれません");
		}
	}

	public GuestStatus getStatus() {
		return status;
	}

	//延滞客にする（同じ客のまま状態だけ変える）
	public void postpone() {
		status = GuestStatus.POSTPONE;
	}

	//通常客に戻す
	public void clearPostpone() {
		status = GuestStatus.NORMAL;
	}

	public boolean getResister() {
		return register;
	}

	public String getName() {
		return name;
	}

	public void setRegister() {
		this.register = true;
	}

	public void checkOverDay(DVD dvd) {
		if (dvd.getOverDay()) {
			postpone();//延滞客に分類された
		}
	}

	//続き
	public void displayRegister() {
		
		if (register) {
			System.out.println(name + "様");

			if (dvdSave.size() == 0) {
				System.out.println("借りているDVDはありません");
			} else {
				System.out.println("借りているDVDは");
				for (int i = 0; i < dvdSave.size(); i++) {
					DVD d = dvdSave.get(i);
					System.out.println(d.getName());
				}
			}

			if (status == GuestStatus.POSTPONE) {
				System.out.println("延滞客");
			} else {
				System.out.println("通常客");
			}
		}
	}
}
