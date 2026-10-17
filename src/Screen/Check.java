package Screen;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import DVD.DVD;
import Guest.Guest;
import Guest.GuestStatus;

public class Check implements Menu{

	Guest guest;
	
	private LocalDate today;

	public Check(Guest guest, LocalDate today) {
	    this.guest = guest;
	    this.today = today;
	}
	
	@Override
	public void display(Scanner sc) {
		
		List<DVD> dvdSave =guest.getDvdSave();
		if (dvdSave.size() == 0) {
			System.out.println("現在借りてるのは、ありません");
		} else {

			System.out.println("現在借りているのは");
			for (DVD d : dvdSave) {
				System.out.println(d.getName() + "\s" + d.getType().getPrice() + "円");
				
				System.out.println("返却日;"+d.getDate());
				if(d.getDate().isBefore(today)) {
					System.out.println("返却期限をすぎています");
				}
				
			}
		}

		if (guest.getResister()) {
			System.out.println("会員登録者です");
		} else {
			System.out.println("会員登録者ではない");
		}
		
		
		if(guest.getStatus() == GuestStatus.POSTPONE) {
			System.out.println("あなたは、延滞客として処理されます");
		}else {
			System.out.println("通常客です");
		}
	}
	
	@Override
	public Guest productGuest(List<Guest> list) {
		return null;
	}
	
}
