package Screen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import DVD.DVD;
import DVD.Detail;
import Guest.Guest;
import Manager.ZaikoKanri;

public class Search implements Menu {

	private Guest guest;
	private ZaikoKanri zaiko;
	
	// キーはタイトル(String)、値は Detail
	private Map<String, Detail> map = new LinkedHashMap<>();

	public Search(Guest guest,ZaikoKanri zaiko) {
		this.guest = guest;
		this.zaiko=zaiko;
		
		DVD[] d=zaiko.getDVD();//戻り値DVD[]
		
		for(DVD dvd:d) {
			//意味不明
			map.put(dvd.getName(), dvd.getDetail().get(0));
		}
	}

	@Override
	public void display(Scanner sc) {
		System.out.println("作品について、詳しく調べます\n"
				+ "タイトル名を入力してください");

		String title = sc.nextLine().trim();
		if (title.isEmpty()) {
			System.out.println("タイトル名が空です");
			return;
		}

		if (map.containsKey(title)) {
			System.out.println("見つかりました");
			Detail detail = map.get(title);
			detail.display();
		} else {
			System.out.println("見つかりませんでした");
		}
	}

	@Override
	public Guest productGuest(List<Guest> list) {
		return null;
	}

}
