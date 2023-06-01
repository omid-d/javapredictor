package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize, BHRSize);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PAPHT = new PerAddressPredictionHistoryTable(branchInstructionSize,(int) Math.pow(2,BHRSize),SCSize);

        // Initialize the SC register
        Bit[] SCbits = new Bit[SCSize];
        for (int i=0;i<SCSize;i++)SCbits[i]=Bit.ZERO;
        SC = new SIPORegister("SC",SCSize, SCbits);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        Bit[] bits1=getCacheEntry(branchInstruction.getInstructionAddress(),PABHR.read(branchInstruction.getInstructionAddress()).read());
        PAPHT.putIfAbsent(bits1, getDefaultBlock());
        Bit[] bits=PAPHT.get(getCacheEntry(branchInstruction.getInstructionAddress(),PABHR.read(branchInstruction.getInstructionAddress()).read()));
        SC.load(bits);
        if(SC.read()[0]==Bit.ONE) {
            return BranchResult.TAKEN;
        }else return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        Bit[] newBits=CombinationalLogic.count(SC.read(),actual==BranchResult.TAKEN,CountMode.SATURATING);
        SC.load(newBits);
        PAPHT.put(getCacheEntry(instruction.getInstructionAddress(),PABHR.read(instruction.getInstructionAddress()).read()), newBits);
        Bit bit;
        ShiftRegister a=PABHR.read(instruction.getInstructionAddress());
        if(actual==BranchResult.TAKEN)bit=Bit.ONE;
        else bit=Bit.ZERO;
        a.insert(Bit.of(BranchResult.isTaken(actual)));
        PABHR.write(instruction.getInstructionAddress(),a.read());
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
