package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {
        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
        Bit[] bits = new Bit[BHRSize];
        for (int i=0;i<BHRSize;i++)bits[i]=Bit.ZERO;
        this.BHR=new SIPORegister("GAgBHR",BHRSize,bits);
        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable((int) Math.pow(2,BHRSize),2);
        // Initialize the SC register
        Bit[] SCbits = new Bit[SCSize];
        for (int i=0;i<SCSize;i++)SCbits[i]=Bit.ZERO;
        SC = new SIPORegister("SC",SCSize, SCbits);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        SC.load(PHT.get(BHR.read()));
        if(SC.read()[0]==Bit.ONE) {
            return BranchResult.TAKEN;
        }else return BranchResult.NOT_TAKEN;
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        Bit[] newBits=CombinationalLogic.count(SC.read(),actual==BranchResult.TAKEN,CountMode.SATURATING);
        SC.load(newBits);
        PHT.put(BHR.read(),newBits);
        Bit bit;
        if(actual==BranchResult.TAKEN)bit=Bit.ONE;
        else bit=Bit.ZERO
        BHR.insert(bit);
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
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
