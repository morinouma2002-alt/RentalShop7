package Exception;

//チェック例外、自分専用の例外
public class RentalException extends Exception{
	
	public RentalException(String message) {
		super(message);
	}

}
