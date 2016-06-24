import socketserver
from contextlib import closing
import time


class MyTCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        print('{} connected'.format(self.client_address[0]))
        while True:
            msg = 'Fuck you dude\n'
            self.request.sendall(msg.encode('utf8'))
            time.sleep(1)


if __name__ == '__main__':
    HOST, PORT = '192.168.1.16', 9999

    with closing(socketserver.TCPServer((HOST, PORT), MyTCPHandler)) as server:
        server.serve_forever()
