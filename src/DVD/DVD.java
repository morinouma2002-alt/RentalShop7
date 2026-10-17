package DVD;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import Exception.RentalException;

public class DVD {

	static Scanner sc = new Scanner(System.in);
	private String name;
	private String value11;
	private String direc;
	
	private DVDPrice type;
	private boolean rented = true;//現在借りられていない場合はtrue

	
	//返却日の管理
	private int oneDay = 0;
	private boolean overDay = false; 
	private LocalDate rentedDay;
	
	List<Detail> list =new ArrayList<>();
	public DVD(String name, String director, String value, int month, int day) {

		this.name = name;
		value11=value;
		this.direc = director;
		
		
		if(value.equals("新作")) {
			this.type=DVDPrice.NEW;
		}else if(value.equals("普通")) {
			this.type=DVDPrice.NORAML;
		}else if(value.equals("旧作")){
			this.type=DVDPrice.OlD;
		}else {
			System.out.println("初期化できませんでした");
			return;
		}

		Detail detail = new Detail(direc, month, day, rented);
		list.add(detail);
	}

	public String getName() {
		return name;
	}

	public DVDPrice getType() {
		return type;
	}
	
	public String getDirec() {
		return direc;
	}

	public boolean getRented() {
		return rented;
	}

	public boolean getOverDay() {
		return overDay;//延滞を返す
	}
	
	public List<Detail> getDetail(){
		return list;
	}
	
	public LocalDate getDate() {
		//unmodifiableListは　LocalDateに適用されない
		return rentedDay;
	}

	public void setOneDay(LocalDate today) {
		
		if(today.isAfter(rentedDay)){
			System.out.println("延滞客に分類されます");
			overDay=true;
		}
		/*oneDay++;

		if (oneDay > 2) {
			System.out.println("あなたは、延滞客に分類されます");
			overDay = true;
		}*/
	}

	public void returnRented() {
		rented = true;
		oneDay=0;
		overDay=false;
	}

	public void rented(LocalDate today) throws RentalException{

		if (rented == false) {
			throw new RentalException("その作品は、借りられているので無理です");
		}
		rented = false;//借りられた
		this.rentedDay=today.plusDays(7);
		System.out.println("返却日は"+rentedDay+"です");

	}

	public void display() {

		if (rented) {
			System.out.println("作品名は :" + name);
			System.out.println("価値は :" + value11);
			System.out.println();
		} else {
			System.out.println("現在商品は借りられています");
			System.out.println();
		}

	}

}
