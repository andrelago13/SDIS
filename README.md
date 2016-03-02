# SDIS

First project for the "Distributed Systems" course (SDIS) of the Integrated Master's in Informatics and Computer Engineering from the Faculty of Engineering of the University of Porto.

Assumptions for the system:
- There may be more than 1 server per computer
- Each server has an unique and constant identifier (numeric)
- The network may loose messages, but retransmissions will eventually get the job done
- Servers may crash but should recover
- Server failure may lead to loss of the server's own files or files it had backed up for other servers
- Metadata isn't lost when servers crash
- Servers are "friendly" and don't try to monopolize the system
