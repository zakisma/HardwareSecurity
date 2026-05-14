import oscilloscope
import serial
import serial.tools.list_ports
from time import sleep

SAMPLE_FREQ = 625*10**6

RO_CNT = 64
TRNG_PAIR_CNT = 64

def list_resources(resources: list, resource_name: str):
    if not resources:
        print('no', resource_name, 'available')
    else:
        print('available', resource_name + ':')
        for resource_id in range(len(resources)):
            print('[', resource_id, '] ', resources[resource_id])

def list_scopes():
    found_oscilloscopes = oscilloscope.get_oscilloscopes()
    list_resources(found_oscilloscopes, 'oscilloscopes')

def channel_meas(scope, n):
    scope.command_check(":WAVeform:SOURce", 'CHANnel{}'.format(n))
    trace = scope.query_binary(':WAVeform:DATA?')
    return trace


# Infinite test run
# The cycle iterates over all ROs. Can be interrupted by pressing CTRL-C
def run(fpga_comm):
    print ("Infinite run, press CTRL-C to break.")
    try:
        i = 0
        while True:
            fpga_comm.write(bytes([i,i]))
            i = (i + 1) % RO_CNT
            sleep(1)
    except KeyboardInterrupt:
        pass
    

def trng_read(scope, fpga_comm):

    with open('data_info.txt', "w") as finfo, open ('data.bin', "wb") as fdata:
    
        for i in range(TRNG_PAIR_CNT):
            print('--------------------------MEAS {}-------------------------------'.format(i))
            scope.write(':SINGle')
            sleep(1)
            fpga_comm.write(bytes([i,i]))
            trace1 = channel_meas(scope, 1)
            trace2 = channel_meas(scope, 2)
            if i == 0:
                tracelength = scope.query(':WAVeform:POINts?')
                fs = scope.query(':ACQuire:SRATe?')
                print(tracelength, file = finfo)
                print(int(float(fs)), file = finfo)
            fdata.write(trace1)
            fdata.write(trace2)

        val = fpga_comm.read(16)
        print(val.hex())
        print(val.hex(), file = finfo)


def test(scope, fpga_comm):
    
    
    with open('data_info.txt', "w") as finfo, open ('data.bin', "wb") as fdata:
        scope.write(':SINGle')
        sleep(1)
        fpga_comm.write(bytes([0,0]))
        trace1 = channel_meas(scope, 1)
        trace2 = channel_meas(scope, 2)
        tracelength = scope.query(':WAVeform:POINts?')
        fs = scope.query(':ACQuire:SRATe?')
        print(tracelength, file = finfo)
        print(int(float(fs)), file = finfo)
        fdata.write(trace1)
        fdata.write(trace2)

    print(len(trace1))
    print(len(trace2))
    print(int(tracelength))
    print(int(float(fs)))




if __name__ == '__main__':
    list_scopes()
    ports = serial.tools.list_ports.comports()
    list_resources(ports, "COM")
    s = serial.Serial(ports[0].name, 923076)
    scope = oscilloscope.Oscilloscope(0)
    # scope.setup_measurement()
    # scope.save_conf('scope_setup.conf')
    scope.load_conf('scope_setup.conf')


    # run(s)

    # test(scope, s)
    trng_read(scope, s)
