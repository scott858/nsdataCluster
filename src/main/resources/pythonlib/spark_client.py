import time
import zmq
import socketserver
import nanomsg

import numpy as np

import src.main.protobuf.bms_voltage_pb2 as bmsv


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


class MockAerobmsServer(socketserver.BaseRequestHandler):
    def handle(self):
        count = 0
        print('{} connected'.format(self.client_address[0]))
        bms_voltage = bmsv.BmsVoltage()
        bms_voltage.device_id = 1
        while True:
            bms_voltage.real_time = int(1000000 * time.time())
            bms_voltage.cpu_time = int(1000000 * time.time())
            for cell_index in range(16):
                voltage_key = "voltage_{}".format(cell_index)
                voltage_value = np.random.randint(0, 4096)
                setattr(bms_voltage, voltage_key, voltage_value)

            # msg = bms_voltage.SerializeToString() + bytes([0])
            msg = bms_voltage.SerializeToString()
            msg = "\0".encode() + msg + "\0".encode()
            self.request.sendall(msg)
            # time.sleep(1)
            count += 1
            print(count)


class AeroBmsZeromqServer:
    def __init__(self):
        pass

    @staticmethod
    def serve_forever(host, port):
        context = zmq.Context()
        socket = context.socket(zmq.PUB)
        socket.bind("tcp://{}:{}".format(host, port))

        count = 0
        print('{} connected'.format(port))
        bms_voltage = bmsv.BmsVoltage()
        bms_voltage.device_id = 1
        while True:
            bms_voltage.real_time = int(1000 * time.time())
            bms_voltage.cpu_time = int(1000 * time.time())
            for cell_index in range(16):
                voltage_key = "voltage_{}".format(cell_index)
                voltage_value = np.random.randint(0, 4096)
                setattr(bms_voltage, voltage_key, voltage_value)

            # msg = bms_voltage.SerializeToString() + bytes([0])
            msg = bms_voltage.SerializeToString()
            print(list(msg))
            socket.send(msg)
            time.sleep(1)
            count += 1


class AeroBmsNanomsgServer:
    def __init__(self):
        pass

    @staticmethod
    def serve_forever(host, port):
        with nanomsg.Socket(nanomsg.PUB) as socket:
            socket.bind("tcp://{}:{}".format(host, port))

            count = 0
            print('{} connected'.format(port))
            bms_voltage = bmsv.BmsVoltage()
            bms_voltage.device_id = 1
            while True:
                bms_voltage.real_time = int(1000 * time.time())
                bms_voltage.cpu_time = int(1000 * time.time())
                for cell_index in range(16):
                    voltage_key = "voltage_{}".format(cell_index)
                    voltage_value = np.random.randint(0, 4096)
                    setattr(bms_voltage, voltage_key, voltage_value)

                msg = bms_voltage.SerializeToString()
                print(list(msg))
                socket.send(msg)
                time.sleep(1)
                count += 1

if __name__ == '__main__':
    # HOST, PORT = '192.168.0.4', 9999
    # HOST, PORT = '172.16.0.15', 9999
    # HOST, PORT = '192.168.0.4', 9999
    HOST, PORT = '192.168.1.72', 9999

    # AeroBmsNanomsgServer.serve_forever(HOST, PORT)
    AeroBmsZeromqServer.serve_forever(HOST, PORT)
    # with closing(socketserver.TCPServer((HOST, PORT), MockAerobmsServer)) as server:
    #     server.serve_forever()
