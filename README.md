# Hardware Security and Cryptography Portfolio (BI-HWB)

## Overview
This repository contains a collection of practical assignments and projects focused on hardware security, applied cryptography, and side-channel analysis. The projects were developed as part of the BI-HWB course and demonstrate implementations across software, embedded systems, and hardware platforms.

## Repository Structure

* **`lab01-03_java_card/`**
  * **Topic:** Smart Card Security
  * **Description:** Implementation of a Java Card applet. Includes APDU scripts for communication and testing secure transactions on a simulated smart card environment.

* **`lab04-06_AES_PC/`**
  * **Topic:** Applied Cryptography (Software)
  * **Description:** A standard C++ implementation of the Advanced Encryption Standard (AES) designed for execution on standard PC architectures. Includes CMake build configurations and testing modules.

* **`lab07_AES_ARM/`**
  * **Topic:** Embedded Cryptography
  * **Description:** AES implementation optimized and adapted for execution on ARM-based microcontrollers, exploring architectural constraints and performance metrics.

* **`lab08_DPA/`**
  * **Topic:** Side-Channel Attacks
  * **Description:** Exploration of Differential Power Analysis (DPA). Involves analyzing power consumption traces during cryptographic operations to extract secret keys.

* **`sram/`**
  * **Topic:** Physical Unclonable Functions (PUFs)
  * **Description:** Data analysis of SRAM startup values across multiple chips to evaluate their viability as hardware fingerprints/PUFs. Includes Jupyter notebook templates for statistical evaluation.

* **`trng/`**
  * **Topic:** True Random Number Generators
  * **Description:** Implementation and vulnerability testing of a TRNG on an FPGA. Includes Python scripts for oscilloscope measurement, sample generation, and statistical attack simulations against the generator output.

## Prerequisites & Tools
Depending on the specific module, the following tools are required for building and testing:
* **C/C++ Modules:** CMake, GCC/Clang
* **Python Modules:** Python 3.x, standard scientific libraries (NumPy, SciPy), and specific hardware interfacing libraries (venv provided locally).
* **Smart Card:** Java Card SDK
