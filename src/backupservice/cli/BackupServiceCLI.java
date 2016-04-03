package backupservice.cli;

import java.io.IOException;

import backupservice.BackupService;


public class BackupServiceCLI {

	public static void main(String[] args) {
		if(args == null || args.length != 8) {
			System.out.println("" + '\n' + '\t' + "=====> BackupService Command Line Interface <=====" + '\n');
			System.out.println("Usage: java BackupServiceCLI <peer_id> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port> <enhanced_mode>");
			System.out.println('\t' + "<peer_id> - Integer representing the peer's unique identifier.");
			System.out.println('\t' + "<mc_addr> - IP address of the CONTROL channel");
			System.out.println('\t' + "<mc_port> - port for the CONTROL channel");
			System.out.println('\t' + "<mdb_addr> - IP address of the BACKUP channel");
			System.out.println('\t' + "<mdb_port> - port for the BACKUP channel");
			System.out.println('\t' + "<mdr_addr> - IP address of the RESTORE channel");
			System.out.println('\t' + "<mdr_port> - port for the RESTORE channel");
			System.out.println('\t' + "<enhanced_mode> - 0/1 to turn enhanced mode off/on, respectively");
			return;
		} else {
			int id;
			String mc_address;
			int mc_port;
			String mdb_address;
			int mdb_port;
			String mdr_address;
			int mdr_port;
			Boolean enhanced_mode;
			
			try {
				id = Integer.parseInt(args[0]);

				mc_address = args[1];
				mc_port = Integer.parseInt(args[2]);
				mdb_address = args[3];
				mdb_port = Integer.parseInt(args[4]);
				mdr_address = args[5];
				mdr_port = Integer.parseInt(args[6]);
				
				int enhanced_mode_int = Integer.parseInt(args[7]);
				enhanced_mode = (enhanced_mode_int == 1);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Invalid arguments. Expected \"<peer_id> <mc_address>:<mc_port> <mdb_address>:<mdb_port> <mdr_address>:<mdr_port> <0/1 - enhanced mode off/on>\".");
				return;
			}
			
			if(enhanced_mode) {
				System.out.println("[CLI]: Starting peer " + id + " with parameters " + mc_address + ":" + mc_port + " " + mdb_address + ":" + mdb_port + " " + mdr_address + ":" + mdr_port + " ENH_MODE:ON");
			} else {
				System.out.println("[CLI]: Starting peer " + id + " with parameters " + mc_address + ":" + mc_port + " " + mdb_address + ":" + mdb_port + " " + mdr_address + ":" + mdr_port + " ENH_MODE:OFF");
			}
		
			try {
				BackupService backup = new BackupService(id, mc_address, mc_port, mdb_address, mdb_port, mdr_address, mdr_port, enhanced_mode);
				backup.initiate();
			} catch (IllegalArgumentException | IOException e) {
				e.printStackTrace();
				System.out.println("[CLI]: Backup service ended with error.");
				return;
			}
		}
		
		System.out.println("[CLI]: Backup service initialization successful!");
	}
}