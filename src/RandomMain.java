import java.io.File;
import java.util.Scanner;

public class RandomMain {
	
	public static void main(String[] args) {
		RandomGenerator mozart = new RandomGenerator();
		try {
		    mozart.writeSequence();
		    System.out.println("Performing...");
			mozart.playSequence();
			
			Thread.sleep(mozart.getLengthInMilliseconds());
			System.out.println("Encore!");
			Scanner inputStream = new Scanner(System.in);
			System.out.print("Save file? (Y/N): ");
			String userAnswer = inputStream.nextLine();
			if(userAnswer.toLowerCase().charAt(0) == 'y') {
			    System.out.print("Enter file name: ");
			    userAnswer = inputStream.nextLine();
			    File song = mozart.saveSequence(userAnswer);
			    System.out.println("File saved at " + song.getAbsolutePath());
			} else {
			    System.out.println("The masterpiece has been lost forever.");
			}
			inputStream.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
