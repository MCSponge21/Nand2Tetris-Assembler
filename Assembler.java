import java.io.*;
import java.util.*;

public class Assembler {

    private enum InstructionType{
        A_INSTRUCTION,
        C_INSTRUCTION,
        L_INSTRUCTION
    } 

    private static Map<String, Integer> SymbolTable = new HashMap<>();

    private static void initializeSymbolTable() {
        SymbolTable.put("SP", 0);
        SymbolTable.put("LCL", 1);
        SymbolTable.put("ARG", 2);
        SymbolTable.put("THIS", 3);
        SymbolTable.put("THAT", 4);
        SymbolTable.put("SCREEN", 16384);
        SymbolTable.put("KBD", 24576);
        SymbolTable.put("R0", 0);
        SymbolTable.put("R1", 1);
        SymbolTable.put("R2", 2);
        SymbolTable.put("R3", 3);
        SymbolTable.put("R4", 4);
        SymbolTable.put("R5", 5);
        SymbolTable.put("R6", 6);
        SymbolTable.put("R7", 7);
        SymbolTable.put("R8", 8);
        SymbolTable.put("R9", 9);
        SymbolTable.put("R10", 10);
        SymbolTable.put("R11", 11);
        SymbolTable.put("R12", 12);
        SymbolTable.put("R13", 13);
        SymbolTable.put("R14", 14);
        SymbolTable.put("R15", 15);
    }

    private static Map<String, String> DestTable = new HashMap<>();
    private static Map<String, String> CompTable = new HashMap<>();
    private static Map<String, String> JumpTable = new HashMap<>();

    private static void initializeJumpTable(){
        JumpTable.put("JGT", "001");
        JumpTable.put("JEQ", "010");
        JumpTable.put("JGE", "011");
        JumpTable.put("JLT", "100");
        JumpTable.put("JNE", "101");
        JumpTable.put("JLE", "110");
        JumpTable.put("JMP", "111");
    }

    private static void initializeDestTable(){
        DestTable.put("M", "001");
        DestTable.put("D", "010");
        DestTable.put("MD", "011");
        DestTable.put("A", "100");
        DestTable.put("AM", "101");
        DestTable.put("AD", "110");
        DestTable.put("AMD", "111");
        DestTable.put("", "000");
    }

    private static void initializeCompTable(){
        CompTable.put("0", "0101010");
        CompTable.put("1", "0111111");
        CompTable.put("-1", "0111010");
        CompTable.put("D", "0001100");
        CompTable.put("A", "0110000");
        CompTable.put("!D", "0001101");
        CompTable.put("!A", "0110001");
        CompTable.put("-D", "0001111");
        CompTable.put("-A", "0110011");
        CompTable.put("D+1", "0011111");
        CompTable.put("A+1", "0110111");
        CompTable.put("D-1", "0001110");
        CompTable.put("A-1", "0110010");
        CompTable.put("D+A", "0000010");
        CompTable.put("D-A", "0010011");
        CompTable.put("A-D", "0000111");
        CompTable.put("D&A", "0000000");
        CompTable.put("D|A", "0010101");
        CompTable.put("M", "1110000");
        CompTable.put("!M", "1110001");
        CompTable.put("-M", "1110011");
        CompTable.put("M+1", "1110111");
        CompTable.put("M-1", "1110010");
        CompTable.put("D+M", "1000010");
        CompTable.put("D-M", "1010011");
        CompTable.put("M-D", "1000111");
        CompTable.put("D&M", "1000000");
        CompTable.put("D|M", "1010101");
    }

    private static int symbolCount = 16;
    private static int lineCount = 0;

    private static void processFile(String assemblyFile, boolean isFirstPass) {
        try (FileReader fileReader = new FileReader(assemblyFile)) {
            Scanner scanner = new Scanner(fileReader);
            PrintWriter writer = isFirstPass ? null : new PrintWriter(new FileWriter("binary.hack"));
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
    
                if (isFirstPass) {
                    processFirstPassLine(line);
                    if (getInstructionType(line) == InstructionType.A_INSTRUCTION || 
                        getInstructionType(line) == InstructionType.C_INSTRUCTION) {
                        lineCount++;
                    }
                } else {
                    InstructionType instructionType = getInstructionType(line);
                    String code = processInstruction(line, instructionType);
                    writer.println(code);
                    System.out.println("Wrote to file: " + code + " from: " + line);
                }
            }
    
            if (writer != null) {
                writer.close();
            }
            scanner.close();
    
            if (isFirstPass) {
                System.out.println(SymbolTable);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    
    public static void main(String[] args) {
        initializeDestTable();
        initializeCompTable();
        initializeJumpTable();
        initializeSymbolTable();
    
        String assemblyFile = args[0];
        processFile(assemblyFile, true);  
        processFile(assemblyFile, false); 
    }

    private static void processFirstPassLine(String line){

        if(line.startsWith("(") && line.endsWith(")")){
            line = line.replace("(", "");
            line = line.replace(")", "");
            
            if(SymbolTable.get(line) == null){
                SymbolTable.put(line, lineCount);
            }
        }
    }

    private static String processInstruction(String line, InstructionType instructionType){
        String code = "";
        switch (instructionType) {
            case A_INSTRUCTION:
                int value;
                line = line.replace("@", "");
                try{
                    value = Integer.parseInt(line);
                }catch(NumberFormatException e){
                    if(SymbolTable.get(line) == null){
                        SymbolTable.put(line, symbolCount);
                        symbolCount++;
                    }
                    value = SymbolTable.get(line);
                }
                
                code = String.format("%16s", Integer.toBinaryString(value)).replace(' ', '0');
                break;
            case C_INSTRUCTION:
                code = "111";

                String dest = getDest(line);
                String comp = getComp(line);
                String jump = getJump(line);

                code = code+comp+dest+jump;
                
                break;
            default:
                break;
        }

        return code;
    }

    //takes the line and returns the dest in binary
    private static String getDest(String line){
        String dest = "000";
        if(line.contains("=")){
            dest = line.substring(0, line.indexOf("="));
            return DestTable.get(dest);
        }
        return dest;
    }
    //does the same thing but with comp
    private static String getComp(String line){
        String comp = "";
        if(line.contains("=") && line.contains(";")){
            comp = line.substring(line.indexOf("=")+1, line.indexOf(";"));
        }else if(line.contains("=")){
            comp = line.substring(line.indexOf("=")+1, line.length());
        }else if(line.contains(";")){
            comp = line.substring(0, line.indexOf(";"));
        }else{
            comp = line;
        }
        return CompTable.get(comp);
    }
    //takes whole line and returns the jump
    private static String getJump(String line){
        String jump = "000";
        if(line.contains(";")){
            jump = line.substring(line.indexOf(";")+1, line.length());
            return JumpTable.get(jump);
        }
        return jump;
    }

    private static InstructionType getInstructionType(String line){
        if(line.startsWith("@")){
            return InstructionType.A_INSTRUCTION;
        }else if(line.startsWith("(")){
            return InstructionType.L_INSTRUCTION;
        }else{
            return InstructionType.C_INSTRUCTION;
        }
    }
}