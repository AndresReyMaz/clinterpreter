import static cTools.KernelWrapper.*;
import java.util.ArrayList;
import java.io.File;
class head {
	public static final String PATH = "./:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin";
	public static final String[] pathList = PATH.split(":");
	public static final String[] fileNotExists = {"head: cannot open '","' for reading: No such file or directory"};
	public static final String[] unrecOption = {"head: unrecognized option '--" , "'\nTry 'head --help' for more information."};
	public static final String[] invalidOption = {"head: invalid option -- '", "'\nTry 'head --help' for more information."};
	public static final String[] argRequired = {"head: option requires an argument -- '","'\nTry 'head --help' for more information."};
	public static final String helpMessage = "Usage: head [OPTION]... [FILE]...\nPrint the first 10 lines of each FILE to standard output.\nWith more than one FILE, precede each with a header giving the file name.\n\nWith no FILE, or when FILE is -, read standard input.\n\nMandatory arguments to long options are mandatory for short options too.\n  -c, --bytes=[-]NUM       print the first NUM bytes of each file;\n                             with the leading '-', print all but the last\n                             NUM bytes of each file\n  -n, --lines=[-]NUM       print the first NUM lines instead of the first 10;\n                             with the leading '-', print all but the last\n                             NUM lines of each file\n  -q, --quiet, --silent    never print headers giving file names\n  -v, --verbose            always print headers giving file names\n  -z, --zero-terminated    line delimiter is NUL, not newline\n      --help     display this help and exit\n      --version  output version information and exit\n\nGNU coreutils online help: <http://www.gnu.org/software/coreutils/>\nFull documentation at: <http://www.gnu.org/software/coreutils/head>\nor available locally via: info '(coreutils) head invocation'";

	public static boolean c_opt = false;
	public static int c_len = 0;
	public static boolean n_opt = false;
	public static int n_len = 10;
	public static boolean read_stdin = false;
	public static int rc_glob = 0;
	public static int n_args = 0;

	/* parse.
	 * 
	 * This function takes as argument a list of parameters
	 * that are either filenames or options (only -c and -n are supported).
	 * That means that args[0] cannot be "head".
	 * It will return a list of all the filenames, while setting the 
	 * global variables that indicate options, if any are present.
	 */
	public static ArrayList<String> parse(String[] args){
		ArrayList<String> files = new ArrayList<String>();
		for(int i = 0; i < args.length; i++){
			//System.out.println(i + ": " +args[i]);
			if(args[i].equals("--help")){
				System.out.println(helpMessage);
				exit(0);
			}
			else if(args[i].equals("-c")){
				c_opt = true;
				n_opt = false;
				if(i != args.length-1){
					i++;
					try{
						c_len = Integer.parseInt(args[i]);
					}
					catch(NumberFormatException e){
						System.out.println(argRequired[0] + "c" + argRequired[1]);
						exit(1);
					}
				}
				else{
					System.out.println(argRequired[0] + "c" + argRequired[1]);
					exit(1);
				}
			}
			else if(args[i].equals("-n")){
				n_opt = true;
				c_opt = false;
				if(i != args.length-1){
					i++;
					try{
						n_len = Integer.parseInt(args[i]);
					}
					catch(NumberFormatException e){
						System.out.println(argRequired[0] + "n" + argRequired[1]);
						exit(1);
					}
				}
				else{
					System.out.println(argRequired[0] + "n" + argRequired[1]);
					exit(1);
				}
			}
			else if(args[i].equals("-") && !read_stdin){
				// We found instruction to read from stdin
				read_stdin = true;
			}
			else if(args[i].charAt(0) == '-'){
				System.out.println(invalidOption[0] + args[i] + invalidOption[1]);
				exit(1);
			}
			else{
				files.add(args[i]);
			}
		}
		return files;
	}

	public static void processLines(String fileName){
		int fd = open(fileName,O_RDONLY);
		int lineCount = 0;
		int count = 10000;
		byte[] buf = new byte[10000];
		if(n_len >= 0){ // Print the first 'n_len' lines.
			StringBuilder lastLine  = new StringBuilder();
			while(true){ //loop to read the entire file
				int rc = read(fd, buf, count);
				if(rc == 0){
					//System.out.println("Debug: this is the end of the file.");
					break;
				}
				if(rc == -1){
					System.out.println("An error ocurred when reading from " + fileName);
					rc_glob = 1;
				}
				for(int i = 0; i < rc; i++){
					if((char) buf[i] == '\n'){
						if(lineCount < n_len)
							System.out.println(lastLine);
						//else break;
						lastLine = new StringBuilder();
						lineCount++;
					}
					else
						lastLine.append((char)buf[i]);
				}
				if(lineCount == 0 && rc > 0)
					System.out.println(lastLine);
			}
		}
		else if(n_len < 0){ // Print all but the last 'n_len' lines
			ArrayList<String> myFile = new ArrayList<String>();
			while(true){
				int rc = read(fd, buf, count);
				if(rc == 0){
					// end of file; now we know how many lines to print.
					for(int i = 0; i < lineCount + n_len; i++){
						System.out.println(myFile.get(i));
					}
					break;
				}
				if(rc == -1){
					System.out.println("An error ocurred when reading from " + fileName);
					rc_glob = 1;
				}
				StringBuilder s = new StringBuilder();
				for(int i = 0; i < rc; i++){
					if((char) buf[i] == '\n'){
						myFile.add(s.toString());
						s = new StringBuilder();
						lineCount++;
					}
					else{
						s.append((char)buf[i]);
					}
				}
			}
		}
		close(fd);
		return;
	}

	public static void processBytes(String fileName){
		int fd = open(fileName,O_RDONLY);
		int byteCount = 0;
		int count = 10000;
		char curChar;
		byte[] buf = new byte[10000];
		if(c_len >= 0){ // Print the first 'c_len' bytes.
			StringBuilder lastLine  = new StringBuilder();
			while(true){ //loop to read the entire file
				int rc = read(fd, buf, count);
				if(rc == 0){
					// end of file
					close(fd);
					break;
				}
				if(rc == -1){
					System.out.println("An error ocurred when reading from " + fileName);
					rc_glob = 1;
				}
				for(int i = 0; i < rc; i++){// Iterate through all bytes
					curChar = (char)buf[i];
					if(byteCount < c_len)
						System.out.print(curChar);
					lastLine.append(curChar);
					byteCount++;
				}
			}
		}
		else if(c_len < 0){ // Print all except the last 'c_len' bytes.
			ArrayList<Character> myFile = new ArrayList<Character>();
			while(true){
				int rc = read(fd, buf, count);
				if(rc == 0){
					// end of file
					for(int i = 0; i < byteCount + c_len; i++){
						System.out.print(myFile.get(i));
					}
					close(fd);
					break;
				}
				if(rc == -1)
					exit(-1);
				int i = 0;
				for(i = 0; i < rc; i++){
					myFile.add((char)buf[i]);
					byteCount++;
				}
			}
		}
		return;		
	}

	public static void processFile(String fileName){
		if(c_opt)
			processBytes(fileName);
		else
			processLines(fileName);
	}

	public static void main(String[] args) {
		c_opt = n_opt = read_stdin = false;
		n_len = 10;
		c_len = 0;
		ArrayList<String> list = parse(args);
		if(!(list.isEmpty())){ // Valid files were found in the arguments; proceeding to execute
			if(list.size() == 1){ // Just process one.
				if((new File(list.get(0))).exists())
					processFile(list.get(0));
				else{
					System.out.println(fileNotExists[0] + list.get(0) + fileNotExists[1]);
					rc_glob = 1;
				}
			}
			else{ // More than one filename found.
				for(int i = 0; i < list.size(); i++){
					if((new File(list.get(i))).exists()){
						System.out.println("==> " + list.get(i) + " <==");
						processFile(list.get(i));
						System.out.println();
					}
					else{
						System.out.println(fileNotExists[0] + list.get(i) + fileNotExists[1]);
						rc_glob = 1;
					}
				}
			}
		}
		if(list.isEmpty() || read_stdin){ // Proceeding to read from stdin.
			byte buf[] = new byte[10000];
			int count = 10000;
			while(true){ //loop to read stdin
				StringBuilder lastLine = new StringBuilder();
				int rc = read(1, buf, count);
				if(rc == 0){
					System.out.println("End of stdin");
					break;
				}
				if(rc == -1){
					System.out.println("Error reading from stdin");
				}
				for(int i = 0; i < rc; i++){
					if((char) buf[i] == '\n'){
						processFile(lastLine.toString());
						lastLine = new StringBuilder();
					}
					else
						lastLine.append((char)buf[i]);
				}
			}
		}
		System.out.println("Done.");
		exit(rc_glob);
	}
}