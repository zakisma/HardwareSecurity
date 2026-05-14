import numpy as np

# Generate sample data_info.txt
trace_length = 1000  # number of samples per trace
sampling_freq = 625000000  # 625 MHz sampling
true_value = "a5c7f23d41b690e8f3d21478c9b6a0e5"  # example 128-bit value

with open('data_info.txt', 'w') as f:
    f.write(f"{trace_length}\n")
    f.write(f"{sampling_freq}\n")
    f.write(f"{true_value}\n")

# Generate sample data.bin
# Simulate 128 traces (64 pairs of RO measurements)
# Each RO will oscillate at slightly different frequencies
num_traces = 128
time = np.linspace(0, trace_length/sampling_freq, trace_length)
data = np.zeros((num_traces, trace_length), dtype=np.uint8)

for i in range(num_traces):
    # Generate oscillating signal with random frequency between 100-200 MHz
    freq = np.random.uniform(100e6, 200e6)
    # Add some noise and convert to uint8 range
    signal = (127 * np.sin(2 * np.pi * freq * time) + 128 + np.random.normal(0, 5, trace_length))
    data[i] = np.clip(signal, 0, 255).astype(np.uint8)

# Save to binary file
data.tofile('data.bin')
