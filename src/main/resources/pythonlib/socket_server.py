import socketserver
from contextlib import closing
import time


class MyTCPHandler(socketserver.BaseRequestHandler):

    def handle(self):
        count = 0
        print('{} connected'.format(self.client_address[0]))
        while True:
            msg = 'Fuck you dude {}\n'.format(count)
            msg += msg
            self.request.sendall(msg.encode('utf8'))
            time.sleep(1)
            count += 1


if __name__ == '__main__':
    # HOST, PORT = '192.168.0.4', 9999
    # HOST, PORT = '172.16.0.15', 9999
    HOST, PORT = '192.168.1.72', 9999

    with closing(socketserver.TCPServer((HOST, PORT), MyTCPHandler)) as server:
        server.serve_forever()
