import static cTools.KernelWrapper.*;
import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
class hello {
	public static final String PATH = "./:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin";
	public static final String[] pathList = PATH.split(":");
	
	/* _execv.
	 * Function performs execv with help of the hard-coded path.
	 */
	public static int _execv(String[] argList){
		if(argList.length >= 0){
			for(int i = 0; i < argList.length; i++){
				if(argList[i].equals("<")){
					lessThanLocation = i;
					System.out.println("Opening file " + argList[i+1]);
					stdin_fd = open(argList[i+1],O_RDONLY);
				}
			}
		}
		
		// Find ">" and if found, open file for stdout
		if(argList.length >= 0){
			for(int i = 0; i < argList.length; i++){
				if(argList[i].equals(">")){
					greaterThanLocation = i;
					System.out.println("Opening file " + argList[i+1]);
					stdout_fd = open(argList[i+1],O_WRONLY | O_CREAT | O_TRUNC);
				}
			}
		}
		
		for(int i = 0; i < pathList.length; i++){
			if((new File(pathList[i] + "/" + argList[0])).exists()){
				if(stdin_fd >= 0 && stdout_fd >= 0){ // Both stdin and stdout
					dup2(stdout_fd,STDOUT_FILENO);
					close(stdout_fd);
					dup2(stdin_fd,STDIN_FILENO);
					close(stdin_fd);
					String[] realArgList;
					if(greaterThanLocation > lessThanLocation){
						realArgList = new String[lessThanLocation];
						for(int j = 0; j < lessThanLocation; j++){
							realArgList[j] = argList[j];
						}
					}
					else{
						realArgList = new String[greaterThanLocation];
						for(int j = 0; j < greaterThanLocation; j++){
							realArgList[j] = argList[j];
						}
					}
					int rc = execv(pathList[i] +  "/" + realArgList[0], realArgList);
					if(rc == 0)
						return 0;
				}
				else if(stdout_fd >= 0){ // Only stdout
					dup2(stdout_fd,STDOUT_FILENO);
					close(stdout_fd);
					String[] realArgList = new String[greaterThanLocation];
					for(int j = 0; j < greaterThanLocation; j++){
						realArgList[j] = argList[j];
					}
					int rc = execv(pathList[i] +  "/" + realArgList[0], realArgList);
					if(rc == 0)
						return 0;
				}
				else if(stdin_fd >= 0){ // Only stdin
					dup2(stdin_fd,STDIN_FILENO);
					close(stdin_fd);
					String[] realArgList = new String[lessThanLocation];
					for(int j = 0; j < lessThanLocation; j++){
						realArgList[j] = argList[j];
					}
					int rc = execv(pathList[i] +  "/" + realArgList[0], realArgList);
					if(rc == 0)
						return 0;
				}
				else{ // No stdin or stdout
					int rc = execv(pathList[i] +  "/" + argList[0], argList);
					if(rc == 0)
						return 0;
				}
			}
		}
		System.out.println("Error: command was not found.");
		return -1;
	}
	
	/* printCommands.
	 * Simple function used for debug purposes.
	 * INPUT: ArrayList of strings which contains all the different 
	 * (originally pipe-separated) commands with their resp parameters.
	 * Then it is printed out onto the stdout.
	 */
	public static void printCommands(ArrayList<String[]> q){
		String[] tmp;
		for(int ind = 0; ind < q.size(); ind++){
			tmp = q.get(ind);
			System.out.print("A command is : ");
			for(int i = 0; i < tmp.length; i++)
				System.out.print(tmp[i] + " ");
			System.out.println();
		}
	}
	
	/* getNoCmnds.
	 * Returns the number of commands (number of pipe symbols + 1).
	 */
	public static int getNoCmnds(String[] argList){
		if(argList.length == 0 || argList.length == 1 && argList[0].equals(""))
			return 0;
		int cnt = 1;
		for(int i = 0; i < argList.length; i++)
			if(argList[i].equals("|")) cnt++;
		return cnt;
	}
	
	/* getPipes.
	 * This function takes the regular list of arguments and separates 
	 * the parameters that are divided by pipe symbols into individual
	 * String arrays (where the 0-th index is the command).
	 * Then these are all added to an ArrayList, which is returned.
	 * INPUT: The original list of arguments, as a String array.
	 * OUTPUT: A list which contains all the arguments, having been 
	 * separated.
	 * NOTE: This function does not include the stdin and stdout ("<" 
	 * and ">") and their respective parameters as part of the list 
	 * returned.
	 */
	public static ArrayList<String[]> getPipes(String[] argsList){
		ArrayList<String[]> q = new ArrayList<String[]>();
		int oldind = 0;
		String[] tmp;
		for(int ind = 0; ind < argsList.length; ind++){
			if(argsList[ind].equals("|")){
				// Found pipe
				tmp = new String[(ind-oldind)];
				//Copy contents into tmp
				for(int i = oldind,j=0; i < ind; i++,j++){
					tmp[j] = argsList[i];
				}
				// Add tmp into list
				q.add(tmp);
				// Set new oldind
				oldind = ind + 1;
			}
			if(ind == argsList.length - 1 /*|| argsList[ind].equals("<") || argsList[ind].equals(">")*/){
				// Reached the end of the arguments
				if((ind == argsList.length - 1))
					ind++;
				tmp = new String[(ind-oldind)];
				//Copy contents into tmp
				for(int i = oldind,j=0; i < ind; i++,j++){
					tmp[j] = argsList[i];
				}
				// Add tmp into list
				q.add(tmp);
				break;
			}
		}
		return q;
	}
	
	public static int stdin_fd = -2;
	public static int stdout_fd = -2;
	public static int lessThanLocation = -1;
	public static int greaterThanLocation = -1;
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.print("Welcome to the interactive shell prompt. ");
		while(true){
			System.out.print("Please type a command:\n> ");
			String ln = sc.nextLine();

			// Initializing variables
			String[] argList = ln.split(" ");

			if(argList[0].equals("exit")){
				System.out.println("Exiting the shell.");
				break;
			}
			ArrayList<String[]> allCmnds = getPipes(argList);
				//printCommands(allCmnds);
			stdin_fd = -2;
			stdout_fd = -2;
			lessThanLocation = -1;
			greaterThanLocation = -1;
			int pid;
			final int nCmnds = getNoCmnds(argList);

			/* 
			 * Making pipes work by using forks
			 */
			
			if(nCmnds >= 1){ //We found at least one command
				int oldfds[] = {-1,-1};
				int tmpfds[] = new int[2];

				ArrayList<Integer> pids = new ArrayList<Integer>();

				for(int i = 0; i < nCmnds; i++){
					if(i != nCmnds-1){ //If not last command, pipe()
						pipe(tmpfds);
					}
					pid = fork();

					if(pid <= -1){
						System.out.println("Error occurred!");
						exit(-1);
					}
					else if(pid == 0){ //Child///////////////////////////
						String cmnd[] = allCmnds.get(i); //Command to execute
						if(i != 0){ //input from prev command
							dup2(oldfds[0],STDIN_FILENO);
							close(oldfds[0]);
							close(oldfds[1]);
						}
						if(i != nCmnds-1){ //output to next command
							close(tmpfds[0]);
							dup2(tmpfds[1],STDOUT_FILENO);
							close(tmpfds[1]);// Does this interfere with (see below)?
						}
						_execv(cmnd);
						exit(0);
					}
					else{ //Parent process for pipes///////////////////////
						
						if(i != 0){
							close(oldfds[0]);
							close(oldfds[1]);
						}
						pids.add(pid);
						
						if(i != nCmnds-1){
							// Now the current file descriptors (tmpfds) will be the old file descriptors (oldfds)
							oldfds = Arrays.copyOf(tmpfds,tmpfds.length); // Does this interfere with (see above)?
						}
					}
				}
				for (int i = 0; i < nCmnds; i++) {
					int[] status = new int[1];
					waitpid(pids.get(i), status, 0);
				}
				//exit(0);
			}
			/* end of main program */
		}
	}
}
