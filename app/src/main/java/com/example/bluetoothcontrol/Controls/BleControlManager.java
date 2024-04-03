package com.example.bluetoothcontrol.Controls;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import com.example.bluetoothcontrol.ReadingData.DataItem;
import com.example.bluetoothcontrol.TerminalDevice.TermItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;

public class BleControlManager extends BleManager {
    private static final byte CONFIGURATION_CMD = (byte) 0x04;
    private static final byte READ_STATE_CMD = (byte) 0x85;

    int successfulOperationsCount = 0;
    int failedOperationsCount = 0;

    public static MutableLiveData<ArrayList<DataItem>> requestData = new MutableLiveData<>();
    private static final ArrayList<DataItem> listOfDataItem = new ArrayList<>();
    public static MutableLiveData<ArrayList<TermItem>> requestDataTermItem = new MutableLiveData<>();
    private static final ArrayList<TermItem> listOfTermItem = new ArrayList<>();
    private static final byte RAW_START_MARK = 0x21;
    private static final byte RAW_RD = (byte) 0x81;
    private long startTime;
    private long endTime;
    private String changedMode;
    private boolean battCheck = false;
    private boolean stage = false;
    private boolean verCheck = false;
    private int versionSoft = 0;

    private static final byte WRITE_CMD = (byte) 0x01;
    private static final byte APPLY_CMD = (byte) 0xEE;
    private static final byte BOOT_RD = (byte) 0x81;
    private static final byte RAW_ASK = (byte) 0x00;
    private TimerCallback timerCallback;
    private PinCallback pinCallback;
    private AcceptedCommandCallback acceptedCommandCallback;
    private int sliseSize;
    private static final byte BOOT_MODE_CMD = (byte) 0x12;
    private static final byte BOOT_MODE_SUCCESS = (byte) 0x00;
    private static final int MAX_ADDRESS = 0x1FFFF;
    private static final byte BOOT_MODE_START = (byte) 0x24;
    private static final byte FIRMWARE_CHUNK_CMD = (byte) 0x01;
    private static final int CHUNK_SIZE = 128;
    private static final int CONFIGURATION_SIZE = 16;

    private int writeCommandCount = 0;
    // UUID для Nordic UART Service
    private static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private static BluetoothGattCharacteristic controlRequest;
    private BluetoothGattCharacteristic controlResponse;

    public BleControlManager(@NonNull Context context) {
        super(context);
    }
    @SuppressLint("NewApi")
    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new BleControlManagerGattCallBacks();
    }
    public void startTimer() {
        startTime = System.currentTimeMillis();
        long durationInMillis = 0; // Начальное значение таймера
        if (timerCallback != null) {
            timerCallback.onTick(stage);
        }
        Log.d("BleControlManager", "Start time: " + durationInMillis);
    }

    public void setTimerCallback(TimerCallback callback) {
        this.timerCallback = callback;
    }
    public void setPinCallback(PinCallback callback) {
        this.pinCallback = callback;
    }
    public void setAcceptedCommandCallback(AcceptedCommandCallback callback){
        this.acceptedCommandCallback = callback;
    }
    public void stopTimer() {
        endTime = System.currentTimeMillis();
        long durationInMillis = endTime - startTime;
        if (timerCallback != null) {
            timerCallback.onTick(stage);
        }
        Log.d("BleControlManager", "End time: " + durationInMillis);
    }
    public void sendCommand(String command, EntireCheck entireCheck) {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                byte[] data = command.getBytes();
                writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        .done(device -> {
                            Log.e("BleControlManager", "Command sent: " + command);
                        })
                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send command: " + status))
                        .enqueue();
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }


    // Offset - смещение данных/ length - кол-во байт списать.
    public void readData(int offset, int length,EntireCheck entireCheck) {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                byte command = RAW_RD;
                byte[] adrNumData = new byte[]{RAW_START_MARK, command, (byte) offset, (byte) length};
                // writeCharacteristic
                Log.d("BleControlManager","Entire is " + entireCheck);
                writeCharacteristic(
                        characteristic,
                        adrNumData,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                        .done(device -> Log.e("BleControlManager", "Read Data command sent"))
                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send read command: " + status))
                        .enqueue();
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }

    public void writeData(byte[] data, EntireCheck entireCheck, DataItem dataItem) {
        Log.d("BleControlManager", "connection from write data is " + isConnected());
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            Log.d("BleControlManager", "Accepting " + Arrays.toString(data) + " from ReadingDataFragment");
            if (characteristic != null) {
                byte[] commandData = new byte[data.length + 5]; // Команда + адрес + количество байт данных
                commandData[0] = RAW_START_MARK;
                commandData[1] = WRITE_CMD;
                commandData[2] = (byte) dataItem.getAddress();
                Log.e("BleControlManager","CheckingAddress " + dataItem.getAddress());
                commandData[3] = (byte) data.length; // <num>
                System.arraycopy(data, 0, commandData, 4, data.length); // <data>
                writeCharacteristic(
                        characteristic,
                        commandData,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                        .done(device -> Log.e("BleControlManager", "Write Data command sent " + Arrays.toString(commandData)))
                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send write command: " + status))
                        .enqueue();
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
        writeCommandCount++;
    }

    public void sendApplyCommand() {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            if (characteristic != null) {
                byte[] commandData = new byte[]{RAW_START_MARK, APPLY_CMD};
                writeCharacteristic(
                        characteristic,
                        commandData,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                        .done(device -> {
                            Log.e("BleControlManager", "Apply Command sent");
                            acceptedCommandCallback.onAcc(true);
                        })
                        .fail((device, status) -> {
                            Log.e("BleControlManager", "Failed to send Apply command: " + status);
                            acceptedCommandCallback.onAcc(false);
                        })
                        .enqueue();
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }


    public void sendPinCommand(String pinCode, EntireCheck entireCheck, String mode) {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            if(Objects.equals(mode,"CHECKPIN")){
                ControlViewModel.Companion.getEntireCheckQueue().add(EntireCheck.CHECK_PIN_RESULT);
            }else{
                ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            }

            changedMode = mode;
            if (characteristic != null) {
                // Добавляем префикс "pin." к пин-коду
                String formattedPinCode = "pin." + pinCode;
                byte[] data = formattedPinCode.getBytes();
                writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        .done(device -> {
                            Log.e("BleControlManager", "PIN command sent");
                            // Флаг устанавливается в true после успешной отправки пин-кода

                        })
                        .fail((device, status) -> {
                            Log.e("BleControlManager", "PIN command ncorrect");
                        })
                        .enqueue();
            }
        }
    }
                                                                                            //// BootMode ////

//    public void sendBootModeCommand(EntireCheck entireCheck) {
//        if (isConnected() && controlRequest != null) {
//            BluetoothGattCharacteristic characteristic = controlRequest;
//            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
//            if (characteristic != null) {
//                byte[] commandData = new byte[]{RAW_START_MARK, BOOT_MODE_CMD};
//                writeCharacteristic(
//                        characteristic,
//                        commandData,
//                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//                )
//                        .done(device ->{
//                            Log.e("BleControlManager", "Accept  Boot Mode command" );
//                        })
//                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send Boot Mode command: " + status))
//                        .enqueue();
//            } else {
//                Log.e("BleControlManager", "Control Request characteristic is null");
//            }
//        } else {
//            Log.e("BleControlManager", "Device is not connected");
//        }
//    }
public void loadFirmware(EntireCheck entireCheck) {
    String filePath = ControlFragment.Companion.getSelectedFilePathBin();
    try (InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(filePath))) {
        BluetoothGattCharacteristic characteristic = controlRequest;
        if (isConnected() && characteristic != null) {
            stage = true;
            startTimer();
            long fileSize = inputStream.available(); // Получаем размер файла
            int fullChunksCount = (int) (fileSize / CHUNK_SIZE);
            int remainingBytes = (int) (fileSize % CHUNK_SIZE);

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            for (int i = 0; i < fullChunksCount; i++) {
                bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    writeFirmwareChunk(buffer, i * CHUNK_SIZE, entireCheck); // Передача данных и адреса
                }
            }
            // Отправить остаточные байты
            if (remainingBytes > 0) {
                bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    writeFirmwareChunk(buffer, fullChunksCount * CHUNK_SIZE, entireCheck); // Передача остаточных данных и адреса
                }
            }
            // После отправки всех данных прошивки загружаем конфигурацию
            loadConfiguration();

        } else {
            Log.e("BleControlManager", "Device is not connected or Control Request characteristic is null");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }

}
    private void writeFirmwareChunk(byte[] data, int address, EntireCheck entireCheck) {
        if (isConnected()) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                int totalBytes = data.length;

                // Отправить основные порции данных размером 128 байт
                int offset = 0;
                while (offset < totalBytes) {
                    int chunkSize = Math.min(CHUNK_SIZE, totalBytes - offset);
                    byte[] chunk = Arrays.copyOfRange(data, offset, offset + chunkSize);
                    // Структура: <start> <cmd> <adr> <num> <data>
                    byte[] commandData = new byte[chunk.length + 7]; // заголовок + команда + адрес + количество байт данных
                    commandData[0] = BOOT_MODE_START;
                    commandData[1] = FIRMWARE_CHUNK_CMD; // Код команды для отправки порции данных

                    // Адрес и количество данных устанавливаются
                    byte[] addressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(address).array();
                    System.arraycopy(addressBytes, 0, commandData, 2, 4); // Адрес
                    commandData[6] = (byte) chunk.length; // Количество байт данных
                    System.arraycopy(chunk, 0, commandData, 7, chunk.length); // Данные

                    // Отправка порции данных
                    writeCharacteristic(characteristic, commandData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                            .enqueue();
                    successfulOperationsCount++;
                    Log.e("BleControlManager", String.valueOf(successfulOperationsCount));

                    // Увеличение адреса для следующей порции данных
                    address += chunkSize;

                    offset += chunkSize;
                }
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }

    public void readFirmware(EntireCheck entireCheck) {

            BluetoothGattCharacteristic characteristic = controlRequest;
            if (isConnected() && characteristic != null) {
                int fullChunksCount = (int) ( 8192 / CHUNK_SIZE);
                byte[] buffer = new byte[CHUNK_SIZE];
                for (int i = 0; i < fullChunksCount; i++) {
                        readFirmwareChunk(buffer, i * CHUNK_SIZE, entireCheck); // Передача данных и адреса
                }

            } else {
                Log.e("BleControlManager", "Device is not connected or Control Request characteristic is null");
            }
        }

    private void readFirmwareChunk(byte[] data, int address, EntireCheck entireCheck) {
        if (isConnected()) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                int totalBytes = data.length;

                // считать основные порции данных размером 128 байт
                int offset = 0;
                while (offset < totalBytes) {
                    int chunkSize = Math.min(CHUNK_SIZE, totalBytes - offset);
                    byte[] chunk = Arrays.copyOfRange(data, offset, offset + chunkSize);
                    // Структура: <start> <cmd> <adr> <num> <data>
                    byte[] commandData = new byte[chunk.length + 7]; // заголовок + команда + адрес + количество байт данных
                    commandData[0] = BOOT_MODE_START;
                    commandData[1] = BOOT_RD; // Код команды для отправки порции данных

                    // Адрес и количество данных устанавливаются
                    byte[] addressBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(address).array();
                    System.arraycopy(addressBytes, 0, commandData, 2, 4); // Адрес
                    commandData[6] = (byte) chunk.length; // Количество байт данных
                    System.arraycopy(chunk, 0, commandData, 7, chunk.length); // Данные

                    // Отправка порции данных
                    writeCharacteristic(characteristic, commandData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                            .enqueue();
                    successfulOperationsCount++;
                    Log.e("BleControlManager", String.valueOf(successfulOperationsCount));

                    // Увеличение адреса для следующей порции данных
                    address += chunkSize;

                    offset += chunkSize;
                }
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }

    public void loadConfiguration() {
        String filePath = ControlFragment.Companion.getSelectedFilePathDat();
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(filePath))) {
            byte[] buffer = new byte[CONFIGURATION_SIZE];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == CONFIGURATION_SIZE) {
                // Отправить данные конфигурации по Bluetooth
                writeConfiguration(buffer,EntireCheck.configurationBootMode);
            } else {
                Log.e("BleControlManager", "Invalid configuration file size");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void writeConfiguration(byte[] data,EntireCheck entireCheck) {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                // Структура: <start> <cmd> <adr> <num> <config data>
                byte[] commandData = new byte[data.length + 7]; // Команда + адрес + количество байт данных
                commandData[0] = BOOT_MODE_START;
                commandData[1] = CONFIGURATION_CMD; // Код команды для отправки данных конфигурации
                byte[] address = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array(); // Адрес устанавливается как 0x00000000
                System.arraycopy(address, 0, commandData, 2, 4); // Адрес
                commandData[6] = (byte) data.length; // Количество байт данных
                System.arraycopy(data, 0, commandData, 7, data.length); // Данные
                writeCharacteristic(
                        characteristic,
                        commandData,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                        .done(device -> {
                            Log.e("BleControlManager", "Configuration data sent");
                            stage = true;
                            stopTimer();
                        })
                        .fail((device, status) -> {
                            Log.e("BleControlManager", "Failed to send configuration data: " + status);
                            stage = false;
                            stopTimer();
                        })
                        .enqueue();
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }
    public void readDeviceState(EntireCheck entireCheck, int numIterations) {
        if (isConnected() && controlRequest != null) {
            BluetoothGattCharacteristic characteristic = controlRequest;
            ControlViewModel.Companion.getEntireCheckQueue().add(entireCheck);
            if (characteristic != null) {
                for (int i = 0; i < numIterations; i++) {
                    byte[] commandData = new byte[]{BOOT_MODE_START, READ_STATE_CMD};
                    writeCharacteristic(
                            characteristic,
                            commandData,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                            .done(device -> Log.e("BleControlManager", "Read Device State command sent"))
                            .fail((device, status) -> Log.e("BleControlManager", "Failed to send Read Device State command: " + status))
                            .enqueue();
                }
            } else {
                Log.e("BleControlManager", "Control Request characteristic is null");
            }
        } else {
            Log.e("BleControlManager", "Device is not connected");
        }
    }

    private boolean isSoftwareVersionInRange(String version, String minRange, String maxRange) {
        String[] currentVersionParts = version.split("\\.");
        String[] minRangeParts = minRange.split("\\.");
        String[] maxRangeParts = maxRange.split("\\.");

        // Преобразование строк в числа для сравнения
        int[] currentVersionNumbers = new int[3];
        int[] minRangeNumbers = new int[3];
        int[] maxRangeNumbers = new int[3];

        for (int i = 0; i < 3; i++) {
            currentVersionNumbers[i] = Integer.parseInt(currentVersionParts[i]);
            minRangeNumbers[i] = Integer.parseInt(minRangeParts[i]);
            maxRangeNumbers[i] = Integer.parseInt(maxRangeParts[i]);
        }

        // Сравнение версии программного обеспечения с диапазоном
        for (int i = 0; i < 3; i++) {
            if (currentVersionNumbers[i] < minRangeNumbers[i] || currentVersionNumbers[i] > maxRangeNumbers[i]) {
                return false;
            } else if (currentVersionNumbers[i] > minRangeNumbers[i] && currentVersionNumbers[i] < maxRangeNumbers[i]) {
                return true;
            }
        }

        // Если версия программного обеспечения находится в указанном диапазоне
        return true;
    }
    private int compareSoftwareVersion(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            int part1 = Integer.parseInt(parts1[i]);
            int part2 = Integer.parseInt(parts2[i]);
            if (part1 < part2) return -1;
            if (part1 > part2) return 1;
        }
        return Integer.compare(parts1.length, parts2.length);
    }




    //// BootMode ////

    private String bytesToHexLogs(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        StringBuilder hex = new StringBuilder();
        hex.append("0x");
        while (buffer.hasRemaining()) {
            hex.append(String.format("%02X", buffer.get() & 0xFF));
            if (buffer.hasRemaining()) {
                hex.append(","); // Добавляем запятую, если еще остались байты
            }
        }
        return hex.toString();
    }
    private  String bytesToHex(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        StringBuilder hex = new StringBuilder();
        hex.append("0x");
        while (buffer.hasRemaining()) {
            hex.append(String.format("%02X", buffer.get() & 0xFF));
        }
        return hex.toString();
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    class BleControlManagerGattCallBacks extends BleManagerGattCallback {

        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            BluetoothGattService controlService = gatt.getService(UART_SERVICE_UUID);
            if (controlService != null) {
                controlRequest = controlService.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
                controlResponse = controlService.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
            }
            return controlRequest != null && controlResponse != null;
        }

        @Override
        protected void onDeviceReady() {
            super.onDeviceReady();
            setNotificationCallback(controlResponse).with(notificationCallback);
            enableNotifications(controlResponse).enqueue();
        }

        @Override
        protected void onServicesInvalidated() {
            Log.d("BleControlManager", "Services invalidated. Disconnecting and closing GATT.");
            controlRequest = null;
            controlResponse = null;

            Log.d("BleControlManager", "Disconnecting from device...");
            disconnect().enqueue();
            Log.d("BleControlManager", "Closing GATT...");
            BleControlManager.this.close();

        }

        private final DataReceivedCallback notificationCallback = (device, data) -> {
            // Обработка данных, полученных от controlResponse
            // data - это массив байтов, которые представляют ответ от устройства
            handleResponseData(data.getValue());
        };

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void handleResponseData(byte[] data) {
            EntireCheck entireCheck1 = ControlViewModel.Companion.getEntireCheckQueue().poll();
            if (entireCheck1 == null) {
                Log.d("BleControlManager", "Entire is null");
                return;
            }

            switch (entireCheck1) {
                case SETUP_TIME:
                    handleSetupTime(data);
                    break;
                case RESERV:
                    // Обработка RESERV
                    Log.d("BleControlManager", "RESERV IS NOT UPDATING ");
                    break;
                case POW_VOLT:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handlePowVolt(data);
                    break;
                case CONFIG_WORD:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleConfigWord(data);
                    break;
                case HW_VER:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleHwVer(data);
                    break;
                case SER_NUM:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleSerNum(data);
                    break;
               // case LOCAL_TIME_SH:
                  //  Log.d("BleControlManager","data " + Arrays.toString(data));
                    //handleLocalTimeShift(data); NAK
                  //  break;
               // case CRC32:
                 //   Log.d("BleControlManager","data " + Arrays.toString(data));
                    // handleCrc32(data); NAK
                  //  break;
                case SETUP_OPERATOR:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleSetupOperator(data);
                    break;
                case PIN_CODE_RESULT:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handlePinCodeResult(data);
                    break;
                case CHECK_PIN_RESULT:
                    handlePinCodeCheck(data);
                    break;
                case I_0UA:
                case I_2UA:
                case I_10UA:
                case I_20UA:
                case I_30UA:
                case I_40UA:
                case I_60UA:
                    int value = (entireCheck1.ordinal() - EntireCheck.I_0UA.ordinal()) * 10;// Получаем значение из названия поля
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleCurrentCalibration(data, value);
                    break;
                case Uref:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleUref(data);
                    break;
                case Tref_mV:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleTref_mV(data, EntireCheck.Tref_mV.name());
                    break;
                case R1_Ohm:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleR1_Ohm(data,EntireCheck.R1_Ohm.name());
                    break;
                case Uw:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleUw(data);
                    break;
                case T10ref_C:
                    Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleT10ref_C(data,EntireCheck.T10ref_C.name());
                    break;
                case WRITE:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    handleWriteData(data);
                    break;
                case default_command:
                    //Log.d("BleControlManager","data " + Arrays.toString(data));
                    handleDefaultCommand(data);
                    break;
                case BootModeResponse:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    handleBootWriteResponse(data);
                    break;
                case configurationBootMode:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    handleConfigurationWriteResponse(data);
                    break;
                case writingBootModeData:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    break;
                case batteryLevel:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    handleCheckBattLevel(data,changedMode);
                    break;
                case softVer:
                    Log.d("BleControlManager", " data HEX type " + bytesToHexLogs(data));
                    handleCheckSoftwareVersion(data,changedMode);
                    break;


            }
            requestData.setValue(listOfDataItem);
            requestDataTermItem.setValue(listOfTermItem);
        }


        private void handleTref_mV(byte[] data,  String fieldName) {
            if (data.length >= 8) {
                float value = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                Log.d("BleControlManager", "updating " + fieldName + ": " + value + " мВ");
                DataItem dataItem = new DataItem(String.valueOf(value), " " + " мВ", fieldName, false,0x1C,0x04,DataType.FLOAT);
                listOfDataItem.add(dataItem);
            } else {
                Log.e("BleControlManager", "Received data for " + fieldName + " is too short to process");
            }
        }

        private void handleR1_Ohm(byte[] data, String fieldName) {
            if (data.length >= 8) {
                long value = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                Log.d("BleControlManager", "updating " + fieldName + ": " + value + " Ом");
                DataItem dataItem = new DataItem(String.valueOf(value), " " + " Ом", fieldName, false,0x20,0x04,DataType.UINT32);
                listOfDataItem.add(dataItem);
            } else {
                Log.e("BleControlManager", "Received data for " + fieldName + " is too short to process");
            }
        }

        private void handleUref(byte[] data) {
            String hexValueUref = bytesToHex(Arrays.copyOfRange(data, 4, 8));
            long configWord = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
            Log.d("BleControlManager", "updating ConfigWord " + configWord);
            DataItem dataItemConfig = new DataItem(hexValueUref,Arrays.toString(new long[]{configWord}), "Uref", false,0x24,0x04,DataType.UINT32);
            listOfDataItem.add(dataItemConfig);
        }

        private void handleUw(byte[] data) {
            String hexValueUw = bytesToHex(Arrays.copyOfRange(data, 4, 8));
            long configWord = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
            Log.d("BleControlManager", "updating ConfigWord " + configWord);
            DataItem dataItemConfig = new DataItem(hexValueUw,Arrays.toString(new long[]{configWord}), "UW", false,0x28,0x04,DataType.UINT32);
            listOfDataItem.add(dataItemConfig);
        }

        @SuppressLint("DefaultLocale")
        private void handleT10ref_C(byte[] data, String fieldName) {
            if (data.length >= 8) {
                // Переводим значение в градусы Цельсия, учитывая масштаб x10
                float value = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() / 10.0f;
                Log.d("BleControlManager", "updating " + fieldName + ": " + value + " °C");
                DataItem dataItem = new DataItem(String.format("%.1f", value), " °C", fieldName, false,0x2C,0x04,DataType.UINT32);
                listOfDataItem.add(dataItem);
            } else {
                Log.e("BleControlManager", "Received data for " + fieldName + " is too short to process");
            }
        }

        private void handleCurrentCalibration(byte[] data, int value) {
            float currentCalibration = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            if (!Float.isNaN(currentCalibration)) {
                Log.d("BleControlManager", "updating i_" + value + "uA " + currentCalibration);
                DataItem dataItemPowVolt = new DataItem(String.valueOf(currentCalibration), bytesToHex(data), "CURRENT CALIBRATION " + value + "uA", false,0x00,0x04,DataType.FLOAT);
                listOfDataItem.add(dataItemPowVolt);
            } else {
                DataItem dataItemPowVolt = new DataItem("is NaN", "is NaN", " CURRENT CALIBRATION " +  value  + " uA ", false,0,0x04,DataType.FLOAT);
                listOfDataItem.add(dataItemPowVolt);
                Log.e("BleControlManager", "CURRENT CALIBRATION value is NaN");
            }
        }
        @SuppressLint("SimpleDateFormat")
        private void handleSetupTime(byte[] data) {
            if (data.length >= 8) {
                long setupTimeSeconds = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                Date date = new Date(setupTimeSeconds * 1000); // Переводим секунды в миллисекунды
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
                String formattedDateTime = formatter.format(date);
                Log.d("BleControlManager", "updating SetupTime " + formattedDateTime);
                DataItem dataItemSetupTime = new DataItem(formattedDateTime, bytesToHex(Arrays.copyOfRange(data, 4, 8)), "SETUP TIME", false,0x30,0x04,DataType.UINT32);
                listOfDataItem.add(dataItemSetupTime);
            } else {
                Log.e("BleControlManager", "Received data is too short to process");
            }
        }
        private void handlePowVolt(byte[] data) {
            float powVoltK = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            if (!Float.isNaN(powVoltK)) {
                Log.d("BleControlManager", "updating Pow_Volt " + powVoltK);
                DataItem dataItemPowVolt = new DataItem(String.valueOf(powVoltK),bytesToHex(data), "PowerVoltageCalib", false,0x34,0x04,DataType.FLOAT);
                listOfDataItem.add(dataItemPowVolt);
            } else {
                DataItem dataItemPowVolt = new DataItem("is NaN", "is NaN", "PowerVoltageCalib", false,0,0x04,DataType.FLOAT);
                listOfDataItem.add(dataItemPowVolt);
                Log.e("BleControlManager", "POW_VOLT value is NaN");
            }
        }
        private void handleWriteData(byte[] data) {
            Log.d("BleControlManager", "Ответ от устройства " + Arrays.toString(data) + " " + bytesToHex(data));

            // Проверяем успешность ответа от устройства на команду записи
            boolean successResponse = data != null && data.length > 0 && data[0] == RAW_ASK;

            if (successResponse) {
                writeCommandCount--;
                if (writeCommandCount == 0) {
                    sendApplyCommand();
                }
            } else {
                Log.e("BleControlManager", "Не успешный ответ от устройства на команду записи");
            }
        }


        private void handleConfigWord(byte[] data) {
            String hexValueConfigWord = bytesToHex(Arrays.copyOfRange(data, 4, 8));
            long configWord = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
            Log.d("BleControlManager", "updating ConfigWord " + configWord);
            DataItem dataItemConfig = new DataItem(hexValueConfigWord,Arrays.toString(new long[]{configWord}), "CONFIG WORD", false,0x38,0x04,DataType.UINT32);
            listOfDataItem.add(dataItemConfig);
        }
        private void handleHwVer(byte[] data) {
            String hwVer = new String(Arrays.copyOfRange(data, 4, 20), StandardCharsets.US_ASCII).trim().replaceAll("[\\x00-\\x1F]", "");
            Log.d("BleControlManager", "updating hwVer " + hwVer);
            DataItem dataItemHwVer = new DataItem(hwVer, bytesToHex(data), "HW VERSION", false,0x4C,0x10,DataType.CHAR_ARRAY);
            listOfDataItem.add(dataItemHwVer);
        }
        private void handleSerNum(byte[] data) {
            String serialNumber = new String(Arrays.copyOfRange(data, 4, 20), StandardCharsets.US_ASCII).trim().replaceAll("[\\x00-\\x1F]", "");
            DataItem dataItemSerNum = new DataItem(serialNumber, bytesToHex(data), "SERIAL NUMBER", false,0x5C,0x10,DataType.CHAR_ARRAY);
            listOfDataItem.add(dataItemSerNum);
            Log.d("BleControlManager", "updating serNumb " + serialNumber);
        }
        private void handleSetupOperator(byte[] data) {
            String setupOperator = new String(Arrays.copyOfRange(data, 4, 20), StandardCharsets.US_ASCII).trim().replaceAll("[\\x00-\\x1F]", "");
            DataItem dataItemSetupOp = new DataItem(setupOperator, bytesToHex(data), "SETUP OPERATOR", false,0x3C,0x10,DataType.CHAR_ARRAY);
            listOfDataItem.add(dataItemSetupOp);
            Log.d("BleControlManager", "updating setupOperator " + setupOperator);
        }
        private void handleLocalTimeShift(byte[] data) {
            long localTimeShift = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
            DataItem dataItemLocalTime = new DataItem(bytesToHex(data), Arrays.toString(new long[]{localTimeShift}), "LOCAL TIME SHIFT", false,0x6C,0x04,DataType.UINT32);
            listOfDataItem.add(dataItemLocalTime);
            Log.d("BleControlManager", "updating locTimeShift " + localTimeShift);
        }
        private void handleCrc32(byte[] data) {
            long crc32Value = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
            DataItem dataItemCrc32 = new DataItem(bytesToHex(data), Arrays.toString(new long[]{crc32Value}), "CRC32", false,0X7C,0x04,DataType.UINT32);
            listOfDataItem.add(dataItemCrc32);
            Log.d("BleControlManager", "updating crc32 " + crc32Value);
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void handlePinCodeResult(byte[] data) {
            String pinResponse = new String(data, StandardCharsets.UTF_8);
            if (pinResponse.contains("pin.ok")) {
                Log.d("BleControlManager", "Pin code is correct");
                if(Objects.equals(changedMode,"TERMINAL")){
                    Log.d("BleControlManager", "TERMINAL MODE");
                    ControlViewModel.Companion.readTerminalCommands();
                }else{
                    sendCommand("version",EntireCheck.softVer);
                }
            } else if (pinResponse.contains("pin.error")) {
                Log.d("BleControlManager", "Pin code is incorrect");
                Log.d("BleControlManager", "Pin code is disconnecting");
                disconnect();
            } else {
                Log.e("BleControlManager", "Invalid pin code response: " + pinResponse);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void handlePinCodeCheck(byte[] data) {
            String pinResponse = new String(data, StandardCharsets.UTF_8);
            if (pinResponse.contains("pin.ok")) {
                Log.d("BleControlManager", "Pin code is correct");
                if (pinCallback != null) {
                    pinCallback.onPin("CORRECT");
                }
            } else if (pinResponse.contains("pin.error")) {
                Log.d("BleControlManager", "Pin code is incorrect");
                if (pinCallback != null) {
                    pinCallback.onPin("INCORRECT");
                }
            } else {
                Log.e("BleControlManager", "Invalid pin code response: " + pinResponse);
            }
        }

        private void handleCheckSoftwareVersion(byte[] data,String mode){
            String softWareResponse = new String(data, StandardCharsets.UTF_8);
            if(softWareResponse.contains("hw")){
                Pattern pattern = Pattern.compile("sw:(\\d+\\.\\d+\\.\\d+)");
                Matcher matcher = pattern.matcher(softWareResponse);
                if (matcher.find()) {
                    String softwareVersion = matcher.group(1);
                    Log.d("BleControlManager", "Software Version: " + softwareVersion);
                    if (compareSoftwareVersion(Objects.requireNonNull(softwareVersion), "4.5.0") < 0 && Objects.equals(mode, "RAW")) {
                        // Версия меньше чем 4.5.0
                        // Устанавливаем значение переменной в 108
                        sliseSize = 108;
                        Log.e("BleControlManager", "Software version less then 4.5.0.");
                    } else if (softwareVersion.equals("4.5.0") && Objects.equals(mode, "RAW")) {
                        // Версия равна 4.5.0
                        // Устанавливаем значение переменной в 128
                        sliseSize = 128;
                        Log.e("BleControlManager", "Software version is 4.5.0.");
                        sendCommand("setraw", EntireCheck.default_command);
                    } else {
                        // Проверка версии программного обеспечения на соответствие диапазону
                        if (isSoftwareVersionInRange(Objects.requireNonNull(softwareVersion), "4.0.9", "4.9.9")) {
                            // Версия программного обеспечения находится в диапазоне
                            Log.d("BleControlManager", "Software version is in range.");
                            verCheck = true;
                            sendCommand("battery", EntireCheck.batteryLevel);
                        } else {
                            // Версия программного обеспечения НЕ находится в диапазоне
                            Log.d("BleControlManager", "Software version is not in range.");
                            verCheck = false;
                        }
                    }
                }else {
                    Log.e("BleControlManager", "Software version not found in response.");
                }
            }
        }
        private void handleCheckBattLevel(byte[] data, String mode) {
            String battLevelReponse = new String(data, StandardCharsets.UTF_8);
            if (battLevelReponse.contains(".t")) {
                // Регулярное выражение для извлечения уровня заряда
                Pattern batteryPattern = Pattern.compile("b(\\d+)");
                Matcher batteryMatcher = batteryPattern.matcher(battLevelReponse);
                if (batteryMatcher.find()) {
                    int batteryLevel = Integer.parseInt(Objects.requireNonNull(batteryMatcher.group(1)));
                    // Проверка уровня заряда
                    if (batteryLevel == 3 || batteryLevel == 2) {
                        Log.d("BleControlManager", "Battery level is normal. " + battLevelReponse);
                        battCheck = true;
                        if(Objects.equals(mode, "BOOT")){
                            sendCommand("boot", EntireCheck.default_command);
                        }else if (Objects.equals(mode,"RAW")){
                            sendCommand("setraw", EntireCheck.default_command);
                        }else{
                            Log.e("BleControlManager", "Mode is undefined" + mode);
                        }
                    } else {
                        Log.e("BleControlManager", "Battery level is not normal.");
                        battCheck = false;
                    }
                } else {
                    Log.e("BleControlManager", "Invalid battery response format. " + battLevelReponse);
                    battCheck = false;
                }
            }
        }


        private void handleDefaultCommand(byte[] data) {
            String defaultResponse = new String(data, StandardCharsets.UTF_8);
            if (defaultResponse.contains("setraw.ok")) {
                Log.d("BleControlManager", "RAW correct");
                ControlViewModel.Companion.readDeviceProfile(sliseSize);
            }else if (defaultResponse.contains("setraw.error")) {
                Log.d("BleControlManager", " incorrect command");
            } else if (defaultResponse.contains("boot.ok")) {
                requestConnectionPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH)
                        .done(device -> Log.e("BleControlManager", "Interval request sent"))
                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send Interval request: " + status))
                        .enqueue();
                requestMtu(247)
                        .done(device -> Log.e("BleControlManager", "MTU request sent "))
                        .fail((device, status) -> Log.e("BleControlManager", "Failed to send MTU request: " + status))
                        .enqueue();
                loadFirmware(EntireCheck.BootModeResponse);
                //readFirmware(EntireCheck.writingBootModeData);
                Log.d("BleControlManager", "Device entered firmware update mode successfully");
            } else if (defaultResponse.contains("boot.error")) {
                Log.e("BleControlManager", "Error: Low battery level");
            }else if(defaultResponse.contains("time")){
                TermItem termItem = new TermItem(defaultResponse,"TIME");
                listOfTermItem.add(termItem);
            }else if(defaultResponse.contains("hw")){
                TermItem termItem = new TermItem(defaultResponse,"VERSION");
                listOfTermItem.add(termItem);
            }
            else if(defaultResponse.contains(".t")){
                TermItem termItem = new TermItem(defaultResponse,"BATTERY");
                listOfTermItem.add(termItem);
            }else if(defaultResponse.contains("ser.")){
                TermItem termItem = new TermItem(defaultResponse,"SERIAL NUMBER");
                listOfTermItem.add(termItem);
            }else if(defaultResponse.contains("mac.")){
                TermItem termItem = new TermItem(defaultResponse,"MAC ADDRESS");
                listOfTermItem.add(termItem);
            }else {
                Log.e("BleControlManager", "Invalid response: " + defaultResponse);
            }
        }
        private void handleBootWriteResponse(byte[] data) {
            if (data.length >= 2) {
                byte flag = data[0];
                byte cmd = data[1];

                switch (flag) {
                    case 0x00:
                        Log.d("BleControlManager", "Command accepted");
                        successfulOperationsCount++;
                        break;
                    case 0x01:
                        Log.d("BleControlManager", "Device busy, retry command");
                        failedOperationsCount++;
                        break;
                    case 0x02:
                        Log.d("BleControlManager", "Previous write command failed");
                        failedOperationsCount++;
                        break;
                    case (byte) 0xFF:
                        Log.d("BleControlManager", "Invalid command format or content ");
                        failedOperationsCount++;
                        break;
                    default:
                        Log.e("BleControlManager", "Unknown response flag: " + flag);
                }
            } else {
                Log.e("BleControlManager", "Invalid response data length for WRITE command");
            }
        }
        private void handleConfigurationWriteResponse(byte[] data) {
            if (data.length >= 2) {
                byte flag = data[0];
                byte cmd = data[1];

                switch (flag) {
                    case 0x00:
                        // Добавить необходимые действия при успешном принятии команды записи конфигурации
                        stage = true;
                        stopTimer();
                        Log.e("BleControlManager", "Configuration write command accepted " + bytesToHexLogs(data));
                        break;
                    case (byte) 0xFF:
                        Log.e("BleControlManager", "Configuration write command not accepted, invalid format or content");
                        stage = false;
                        stopTimer();
                        break;
                    default:
                        Log.e("BleControlManager", "Unknown response flag for configuration write command: " + flag);
                        stage = false;
                        stopTimer();
                        break;
                }
            } else {
                Log.e("BleControlManager", "Invalid response data length for configuration write command");
            }
        }


        private void handleLastCommandResultResponse(byte[] data){
            if (data.length >= 2) {
                byte flag = data[0];
                byte cmd = data[1];
                switch (flag) {
                    case 0x00:
                        // Предыдущая команда выполнена успешно
                        Log.d("BleControlManager", "Previous command executed successfully");
                        Log.e("BleControlManager", "Answer from device " + bytesToHexLogs(data));
                        break;
                    case 0x01:
                        // Устройство занято обработкой предыдущей команды
                        Log.e("BleControlManager", "Device is busy processing the previous command");
                        Log.e("BleControlManager", "Answer from device " + bytesToHexLogs(data));
                        break;
                    case 0x02:
                        // Предшествующая команда завершилась с ошибкой
                        Log.e("BleControlManager", "Previous command ended with an error");
                        Log.e("BleControlManager", "Answer from device " + bytesToHexLogs(data));
                        break;
                    default:
                        // Неизвестный флаг
                        Log.e("BleControlManager", "Unknown flag in response: " + flag);
                        Log.e("BleControlManager", "Answer from device " + bytesToHexLogs(data));
                        break;
                }
            } else {
                Log.e("BleControlManager", "Invalid response data length for Read Device State command");
            }
        }
   }
    public interface TimerCallback {
        void onTick(boolean Stage);
    }
    public interface PinCallback {
        void onPin(String pin);
    }
    public interface AcceptedCommandCallback {
        void onAcc(boolean acc);
    }
}