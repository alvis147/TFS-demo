package ads;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.naming.LinkLoopException;
import javax.swing.text.StyledEditorKit.BoldAction;


public class FS {
	public static final String free = "000";
	public static final String allocated = "100";
	public static final String transparent = "110";
	public static final String alloc_over = "101";
	public static final String free_over = "001";
	public final long disk_size = 100*1024*1024;
	public final int block_num=25*1024;
	public final int block_size= (int) (disk_size/block_num);
	public final int inode_size = 64;
	public final int inode_num_max = block_num;
	public final int file_size_max = 5*block_size;//可以保存的文件最大为20k字节
	//一个inodetable条目是64个byte,第一个保存状态
	public final int inode_ft_offset = 1;//文件类型的起始位置
	public final int inode_fn_offset = 2;//inode中文件名的起始位置
	public final int inode_fs_offset = 18;
	public final int inode_bn_offset = 22;//inode中block id的起始位置
	public final int block_first_id = 405;//第一个存储block的下标
	public final String path="D://adsFile";
	
	public byte blocks[][]=new byte[block_num][block_size];
	
	
	//****************工具函数
	// 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序
	public static byte[] intToBytes(int value)   
	{   
	    byte[] src = new byte[4];  
	    src[0] = (byte) ((value>>24) & 0xFF);  
	    src[1] = (byte) ((value>>16)& 0xFF);  
	    src[2] = (byte) ((value>>8)&0xFF);    
	    src[3] = (byte) (value & 0xFF);       
	    return src;  
	} 
	public static byte[] shortToBytes(short value)   
	{   
	    byte[] src = new byte[2];  
	    src[0] = (byte) ((value>>8) & 0xFF);  
	    src[1] = (byte) (value & 0xFF);           
	    return src;  
	} 
	//byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序
	public static int bytesToInt(byte[] src, int offset) {  
	    int value;    
	    value = (int) ( ((src[offset] & 0xFF)<<24)  
	            |((src[offset+1] & 0xFF)<<16)  
	            |((src[offset+2] & 0xFF)<<8)  
	            |(src[offset+3] & 0xFF));  
	    return value;  
	}
	public static short bytesToShort(byte[] src, int offset) {  
	    short value;    
	    value = (short) ( ((src[offset] & 0xFF)<<8)  
	            |(src[offset+1] & 0xFF));  
	    return value;  
	}
	//****************工具函数
	
	
	public byte[] read_block(short blockId){
		if( (blockId<block_first_id) || (blockId>=block_num))
		{
			return null;
		}
		return blocks[blockId];
	}
	
	public void write_block(int blockId, byte[] buf){
		if(! ((blockId<block_first_id) || (blockId>=block_num) || (buf==null)))
			System.arraycopy(buf, 0, blocks[blockId], 0, buf.length);
	}
	
	public String getBinCode(int blockId){//获取blockId对应块的状态
		int relBlockId = blockId-block_first_id;
		int j = relBlockId/8;
		int k = relBlockId%8;
		int x = (blocks[0][j]>>(7-k))&1;
		int y = (blocks[1][j]>>(7-k))&1;
		int z = (blocks[2][j]>>(7-k))&1;
		return ""+x+y+z;
	}
	public int getBlockStatus(int blockId){//获取blockId对应块打开状态 0表示没打开，1表示打开
		int relBlockId = blockId-block_first_id;
		int j = relBlockId/8;
		int k = relBlockId%8;
		int x = (blocks[3][j]>>(7-k))&1;//这里如果左移位数不对的话就8-k
		return x;
	}
	public void changeBlockStatus(int blockId){
		int relBlockId = blockId-block_first_id;
		int j = relBlockId/8;
		int k = relBlockId%8;
		if(getBlockStatus(blockId)==0)
			{blocks[3][j] |=(1<<(7-k));}
		else{
			blocks[3][j] &= ~(1<<(7-k));
		}
	}
	public void modifyBinCode(int blockId,String after){//block alloc之后改变状态
		int relBlockId = blockId-block_first_id;
		int j = relBlockId/8;
		int k = relBlockId%8;
		for(int i=0;i<after.length();i++)
		{
			int m = Integer.parseInt(after.substring(i, i+1));
			if(m==0)
				blocks[i][j] &= ~(1<<(7-k));
			else {
				blocks[i][j] |= (1<<(7-k));
			}
		}
	}
	
	public List<Short> alloc_block(int blockNum){//blockNum是需要的block的数目  write
		List<Short> list = new ArrayList<>();
		if(blockNum<1||blockNum>21)//21是最多可分配的block的数目
			return null;
		for(short i=block_first_id;i<block_num&&blockNum>0;i++){//这里考虑i的初始值
			String string = getBinCode(i);
			if(!string.equals(allocated) && !string.equals(alloc_over)&&getBlockStatus(i)==0){//不是allocateed且没有打开
				list.add(i);
				blockNum--;
				changeBlockStatus(i);
				switch (string) {
				case free:
					modifyBinCode(i, allocated);
					break;
				case free_over:
					modifyBinCode(i, alloc_over);
					break;
				case transparent:
					modifyBinCode(i, alloc_over);
					break;
				}
			}
		}
		return list;
	}
	
	public List<Short> alloc_block_tran(int blockNum){//transparent write
		List<Short> list = new ArrayList<>();
		if(blockNum<1||blockNum>21)//21是最多可分配的block的数目
			return null;
		for(short i=block_first_id;i<block_num&&blockNum>0;i++){
			String string = getBinCode(i);
			if(string.equals(free)&&getBlockStatus(i)==0){
				list.add(i);
				blockNum--;
				changeBlockStatus(i);
				modifyBinCode(i, transparent);
			}
		}
		return list;
	}
	
	public void recycle_block(List<Short> list){//delete
		for(short i : list){
			byte b = 0;
			java.util.Arrays.fill(blocks[i],b);
			switch (getBinCode(i)) {
			case alloc_over:
				modifyBinCode(i, free_over);
				break;
			case allocated:
				modifyBinCode(i, free);
				break;
			}
		}
	}
	
	public void recycle_block_trans(List<Short> list){//transparent delete
		for(short i : list){
			byte b = 0;
			java.util.Arrays.fill(blocks[i],b);
			modifyBinCode(i, free);
		}
	}
	
	public void clean_block(List<Short> list){
		for(short i : list){
			byte b = 0;
			java.util.Arrays.fill(blocks[i], b);
			switch(getBinCode(i)){
			case alloc_over:
				modifyBinCode(i, allocated);
				break;
			case free_over:
				modifyBinCode(i, free);
				break;
			}
		}
		
	}
	//**************************************
	//文件大小用4个字节，文件名用16字节，block下标用（21*2）个字节。
	//inode table从blocks[5]开始。到block[405]
	public List<Short> inode_allocate(String fileName,int fileSize){
		if(fileSize<file_size_max){//文件最大20k?这里需要考虑单位
			List<Short> list= new ArrayList<>();
			for(int i=0;i<inode_num_max;i++){
				int blockId = i*inode_size/block_size;
				int offset = i*inode_size%block_size;
				//放文件大小
				int fileSizeT = bytesToInt(blocks[3+blockId], offset);
				
				System.out.println("filesizeT:"+fileSizeT);
				
				if(fileSizeT==0){//第一个字节保存文件大小，为零说明 为空
					byte[] fileSizeB = intToBytes(fileSize);
					for(int l=0;l<4;l++){
						blocks[3+blockId][0+offset+l] = fileSizeB[l];
					}
					byte[] fname = fileName.getBytes();//放文件名
					int length = fname.length;
					System.out.println("alloc_fname:"+length);
					System.out.println("blockId:"+blockId);
					System.out.println("offset:"+offset);
					for(int j=0;j<(length<8?length:8);j++){
						blocks[3+blockId][0+offset+inode_fn_offset+j]=fname[j];
					}
					
					int need_block_num = (int) Math.ceil((double)fileSize/block_size);
					System.out.println("need:"+need_block_num);
					list = alloc_block(need_block_num);
					for(int k=0;k<list.size();k++){//开始放block下标
						System.out.println("alloc_block:"+list.get(k));
						byte[] block = intToBytes(list.get(k));
						System.out.println("afterturen:"+bytesToInt(block, 0));
						for(int m = 0;m<4;m++){
							blocks[3+blockId][0+offset+inode_bn_offset+k*4+m] = block[m];
						}
						System.out.println("cun wan qu:"+bytesToInt(blocks[3+blockId], offset+inode_bn_offset+k*4));
					}
				}
				break;
			}
			return list;
		}else {
			return null;
		}
	}
	//由文件名获取block下标
	public List<Integer> inode_get(String fileName){
		List<Integer> list = new ArrayList<>();
		Boolean flag = false;
		label1:
		for(int i=0;i<inode_num_max;i++){
			int blockId = i*inode_size/block_size;
			int offset = i*inode_size%block_size;
			int fileSizeT = bytesToInt(blocks[3+blockId], offset);
			if(fileSizeT!=0){
			System.out.println("get_fs:"+fileSizeT);
			//？？这里考虑再分配inode的时候，文件名可能是保存的小于8个字节，获取的时候拿8个会出错吗
			String string = "";
			for(int m = 0 ; m<8;m++){
				if(blocks[3+blockId][offset+inode_fn_offset+m]!=0)
					string+=(char)blocks[3+blockId][offset+inode_fn_offset+m];
			}
			
			if(string.trim().equals(fileName.trim())){
				for(int k=0;k<5;k++){
					int block = bytesToInt(blocks[3+blockId], offset+inode_bn_offset+k*4);
					if(block!=0){
						list.add(block);
					}
				}
				flag = true;
				break label1;
			}
			}
		}
		if(flag==true)
			return list;
		else {
			return null;
		}
	}
	//删除inode
	public void inode_delete(int index){
		int blockId = index*inode_size/block_size;
		int offset = index*inode_size%block_size;
		byte b = 0;
		java.util.Arrays.fill(blocks[5+blockId], offset, offset+inode_size, b);
	}
	//删除对应的inode bit map
	public void inode_map_delete(int index){
		int j = index/8;
		int k = index%8;
			blocks[4][j] &= ~(1<<(7-k));
	}
	//打开文件
	public void openFile(String fileName){
		
		BufferedOutputStream bos = null;  
        FileOutputStream fos = null;  
        File file = null;
        byte[] buf = null;
        
        try {
        	File dir = new File(path);  
            if (!dir.exists() && dir.isDirectory())  
            {  
                dir.mkdirs();  
            }  
            file = new File(path + File.separator + fileName);  
            fos = new FileOutputStream(file);  
            bos = new BufferedOutputStream(fos);  
            List<Integer> list = inode_get(fileName);
    		for(int i:list){
    			bos.write(read_block((short) i));
    		}
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			if(bos!=null){
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
	}
	//写文件
	public void writeFile(String fileName, byte[] b){
		int fileSize = b.length;
		System.out.println("filesize:"+fileSize);
		List<Short> list = inode_allocate(fileName, fileSize);
		int offset = 0;
		for(int i = 0;i<list.size();i++){
			byte[] temp = new byte[block_size];
			if(list.size()>i+1)
			{
				System.out.println("if");
				System.arraycopy(b, offset, temp, 0, block_size);
				write_block(list.get(i), temp);
				offset+=block_size;
			}else {
				System.out.println("else"+" i:"+(fileSize-i*block_size));
				System.arraycopy(b, offset, temp, 0,fileSize-i*block_size );
				write_block(list.get(i), temp);
			}
		}
	}
	
	public void open(String fileName){//打开普通文件
		//首先去inode table找
		boolean exit = false;
		if(fileName.getBytes().length<=16)//文件名最大16个字节
		{
			for(int i=0;i<inode_num_max;i++){
				int blockId = i*inode_size/block_size;
				int offset = i*inode_size%block_size;
				String string = "";
				for(int m = 0 ; m<16;m++){//组装文件名
					if(blocks[5+blockId][offset+inode_fn_offset+m]!=0)
						string+=(char)blocks[5+blockId][offset+inode_fn_offset+m];
				}
				if(fileName.equals(string)){//如果这个文件已存在,就打开它
					blocks[5+blockId][offset]=(byte)1;//改变在inode table中的文件打开状态
					List<Short> list = new ArrayList<>();//获取block下标
					for(int j=0;j<21;j++){
						short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
						if(block!=0)
							list.add(block);
					}
					for(short k : list){//改变在blcok bitmap 中的block打开状态
						changeBlockStatus(k);
					}
					exit = true;
					break;//跳出循环
				}
			}
			if(exit==false){//不存在
					for(int l=0;l<inode_num_max;l++){
						//在inode bitmap 中找一个 inode
						if((blocks[4][l/8]>>(7-l%8)&1)==0){//bitmap为零就可用
							blocks[4][l/8] |=(1<<(7-l%8));//inode bitmap置为1
							int blockId1 = l*inode_size/block_size;
							int offset1 = l*inode_size%block_size;
							blocks[5+blockId1][offset1] = (byte)1;//inode table 项 打开状态
							blocks[5+blockId1][offset1+inode_ft_offset] = (byte)0;//ordinary文件为0
							
							byte[] fname = fileName.getBytes();//放文件名
							int length = fname.length;
							for(int j=0;j<(length<16?length:16);j++){
								blocks[5+blockId1][offset1+inode_fn_offset+j]=fname[j];
							}
							break;
						}
						
					}
			}
		}
	}
	
	public void list_data_bitmap(int start,int length){
		for(int i=0;i<length;i++){
			int byteId = (start+i)/8;
			int offset = (start+i)%8;
			int x = (blocks[0][byteId]>>(7-offset))&1;
			int y = (blocks[1][byteId]>>(7-offset))&1;
			int z = (blocks[2][byteId]>>(7-offset))&1;
			System.out.println(""+x+y+z);
		}
	}
	
	public void list_inode_bitmap(int start,int length){
		for(int i=0;i<length;i++){
			int byteId = (start+i)/8;
			int offset = (start+i)%8;
			int x = (blocks[4][byteId]>>(7-offset))&1;
			System.out.println(""+x);
		}
	}
	public void openTrans(String fileName){//打开transparent文件
		//首先去inode table找
				boolean exit = false;
				boolean fail = false;
				if(fileName.getBytes().length<=16)//文件名最大16个字节
				{
					loop2:
					for(int i=0;i<inode_num_max;i++){
						int blockId = i*inode_size/block_size;
						int offset = i*inode_size%block_size;
						String string = "";
						for(int m = 0 ; m<16;m++){//组装文件名
							if(blocks[5+blockId][offset+inode_fn_offset+m]!=0)
								string+=(char)blocks[5+blockId][offset+inode_fn_offset+m];
						}
						if(fileName.equals(string)){//如果这个文件已存在,就打开它
							if(blocks[5+blockId][offset+inode_ft_offset]==0){
								exit = true;
								System.out.println("这个文件是ordinary");
								break loop2;
							}
							blocks[5+blockId][offset]=(byte)1;//改变在inode table中的文件打开状态
							List<Short> list = new ArrayList<>();//获取block下标
							for(int j=0;j<21;j++){
								short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
								if(block!=0)
									list.add(block);
							}
							
							//在这里判断他的block是否 有被损坏的
							for(int x=0;x<list.size();x++){
								if(getBinCode(list.get(x))!=transparent){//如果有一个块的状态不是transparent
									//删除这个inode，并
									byte b = 0;
									java.util.Arrays.fill(blocks[5+blockId], offset, offset+inode_size, b);
									//inode bitmap 对应置0
									blocks[4][i/8] &= ~(1<<(7-i%8));
									//还是transparent的block设置为free。
									for(int y=0;y<list.size();y++){
										if(getBinCode(list.get(y)).equals(transparent)){
											java.util.Arrays.fill(blocks[list.get(y)], 0, block_size, b);
											//changeBlockStatus(list.get(y));
											modifyBinCode(list.get(y), free);
										}else if (getBinCode(list.get(y)).equals(alloc_over)) {
											modifyBinCode(list.get(y), allocated);
											
										}
									}
									fail = true;
									break loop2;
								}
							}
							
							for(short k : list){//改变在blcok bitmap 中的block打开状态
								changeBlockStatus(k);
							}
							exit = true;
							break;//跳出循环
						}
					}
					if(fail==true)
						System.out.println("文件的block已被占用");
					else if(exit==false){//不存在
							for(int l=0;l<inode_num_max;l++){
								//在inode bitmap 中找一个 inode
								if((blocks[4][l/8]>>(7-l%8)&1)==0){//bitmap为零就可用
									blocks[4][l/8] |=(1<<(7-l%8));//inode bitmap置为1
									int blockId1 = l*inode_size/block_size;
									int offset1 = l*inode_size%block_size;
									blocks[5+blockId1][offset1] = (byte)1;//inode table 项 打开状态
									blocks[5+blockId1][offset1+inode_ft_offset] = (byte)1;//ordinary文件为0
									
									byte[] fname = fileName.getBytes();//放文件名
									int length = fname.length;
									for(int j=0;j<(length<16?length:16);j++){
										blocks[5+blockId1][offset1+inode_fn_offset+j]=fname[j];
									}
									break;
								}
								
							}
					}
				}
	}
	public void close(String fileName){
		if(fileName.getBytes().length<=16)//文件名最大16个字节
		{
			for(int i=0;i<inode_num_max;i++){
				int blockId = i*inode_size/block_size;
				int offset = i*inode_size%block_size;
				String string = "";
				for(int m = 0 ; m<16;m++){//组装文件名
					if(blocks[5+blockId][offset+inode_fn_offset+m]!=0)
						string+=(char)blocks[5+blockId][offset+inode_fn_offset+m];
				}
				if(fileName.equals(string)){//找到这个文件
					blocks[5+blockId][offset]=(byte)0;//改变在inode table中的文件打开状态
					List<Short> list = new ArrayList<>();//获取block下标
					for(int j=0;j<21;j++){
						short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
						if(block!=0)
							list.add(block);
					}
					for(short k : list){//改变在blcok bitmap 中的block打开状态
						changeBlockStatus(k);
					}
					break;//跳出循环
				}
			}
		}
	}
	public int getInodeByfileName(String fileName){
		for(int i=0;i<inode_num_max;i++){
			int blockId = i*inode_size/block_size;
			int offset = i*inode_size%block_size;
			String string = "";
			for(int m = 0 ; m<16;m++){//组装文件名
				if(blocks[5+blockId][offset+inode_fn_offset+m]!=0)
					string+=(char)blocks[5+blockId][offset+inode_fn_offset+m];
			}
			if(fileName.equals(string))
				return i;
			}
		return -1;
	}
	//ordinary文件写
	public void write(String fileName,byte[] b){
		int length = b.length;
		int need_block_num = (int) Math.ceil((double)length/block_size);
		List<Short> list = alloc_block(need_block_num);
		int index = getInodeByfileName(fileName);
		int blockId = index*inode_size/block_size;
		int offset = index*inode_size%block_size;
		byte[] fileSizeB = intToBytes(length);//放文件大小
		for(int l=0;l<4;l++){
			blocks[5+blockId][offset+inode_fs_offset+l] = fileSizeB[l];
		}
		for(int i=0;i< list.size();i++){//放block下标
			byte[] blcokId = shortToBytes(list.get(i));  
			for(int l=0;l<2;l++){
				blocks[5+blockId][offset+inode_bn_offset+i*2+l] = blcokId[l];
			}
		}
		//放完之后开始写入
		int offset1 = 0;
		for(int i = 0;i<list.size();i++){
			byte[] temp = new byte[block_size];
			if(list.size()>i+1)
			{
				System.arraycopy(b, offset1, temp, 0, block_size);
				write_block(list.get(i), temp);
				offset1+=block_size;
			}else {
				System.arraycopy(b, offset1, temp, 0, length-i*block_size);
				write_block(list.get(i), temp);
			}
		}
	}
	//transparent文件写
	public void writeTrans(String fileName,byte[] b){
		int length = b.length;
		int need_block_num = (int) Math.ceil((double)length/block_size);
		List<Short> list = alloc_block_tran(need_block_num);
		int index = getInodeByfileName(fileName);
		int blockId = index*inode_size/block_size;
		int offset = index*inode_size%block_size;
		byte[] fileSizeB = intToBytes(length);//放文件大小
		for(int l=0;l<4;l++){
			blocks[5+blockId][offset+inode_fs_offset+l] = fileSizeB[l];
		}
		for(int i=0;i< list.size();i++){//放block下标
			byte[] blcokId = shortToBytes(list.get(i));  
			for(int l=0;l<2;l++){
				blocks[5+blockId][offset+inode_bn_offset+i*2+l] = blcokId[l];
			}
		}
		//放完之后开始写入
		int offset1 = 0;
		for(int i = 0;i<list.size();i++){
			byte[] temp = new byte[block_size];
			if(list.size()>i+1)
			{
				System.arraycopy(b, offset1, temp, 0, block_size);
				write_block(list.get(i), temp);
				offset1+=block_size;
			}else {
				System.arraycopy(b, offset1, temp, 0, length-i*block_size);
				write_block(list.get(i), temp);
			}
		}
	}
	//读文件
	public void read(String fileName){
	
		int index = getInodeByfileName(fileName);
		if(index!=-1){
			List<Short> list = new ArrayList<>();
			int blockId = index*inode_size/block_size;
			int offset = index*inode_size%block_size;
			for(int k=0;k<21;k++){
				short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+k*2);
				if(block!=0){
					list.add(block);
				}
			}
			String string = "";
			for(int i=0;i<list.size();i++){
				byte[] bs = read_block(list.get(i));
				for(int m = 0 ; m<block_size;m++){//组装文件名
					if(bs[m]!=0)
						string+=(char)bs[m];
				}
			}
			System.out.println(string);
		}
	}
	public void list(String fileName){
		int index = getInodeByfileName(fileName);
		if(index!=-1){
			int blockId = index*inode_size/block_size;
			int offset = index*inode_size%block_size;
			System.out.println("文件名:"+fileName);
			System.out.println("文件类型："+((int)blocks[5+blockId][offset+inode_ft_offset]==1?"transparent":"ordinary"));
			System.out.println("打开状态:"+((int)blocks[5+blockId][offset]==1?"打开":"关闭"));
			System.out.println("文件大小:"+bytesToInt(blocks[5+blockId], offset+inode_fs_offset));
			List<Short> list = new ArrayList<>();//获取block下标
			for(int j=0;j<21;j++){
				short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
				if(block!=0)
					list.add(block);
			}
			System.out.print("block：");
			for(short i : list){
				System.out.print(" "+i);
			}
			}else {
				System.out.println("无此文件");
			}
		System.out.println();
		list_data_bitmap(0, 5);
		list_inode_bitmap(0, 5);
		System.out.println();
	}
	public void delete(String fileName){
		int index = getInodeByfileName(fileName);
		if(index!=-1){
			int blockId = index*inode_size/block_size;
			int offset = index*inode_size%block_size;
			if(blocks[5+blockId][offset+inode_ft_offset]==1){
				System.out.println("这个文件是transparent的");
			}else{
				List<Short> list = new ArrayList<>();//获取block下标
				for(int j=0;j<21;j++){
					short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
					if(block!=0)
						list.add(block);
				}
				recycle_block(list);
				inode_delete(index);
				inode_map_delete(index);
			}
		}else {
			System.out.println("文件不存在");
		}
	}
	
	public void deleteTrans(String fileName){
		int index = getInodeByfileName(fileName);
		if(index!=-1){
			int blockId = index*inode_size/block_size;
			int offset = index*inode_size%block_size;
			if(blocks[5+blockId][offset+inode_ft_offset]==0){
				System.out.println("这个文件是ordinary的");
			}else{
				List<Short> list = new ArrayList<>();//获取block下标
				for(int j=0;j<21;j++){
					short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
					if(block!=0)
						list.add(block);
				}
				recycle_block_trans(list);
				inode_delete(index);
				inode_map_delete(index);
			}
		}else {
			System.out.println("文件不存在");
		}
	}
	public void clean(String fileName){
		int index = getInodeByfileName(fileName);
		if(index!=-1){
			int blockId = index*inode_size/block_size;
			int offset = index*inode_size%block_size;
			List<Short> list = new ArrayList<>();//获取block下标
			for(int j=0;j<21;j++){
				short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
				if(block!=0)
					list.add(block);
			}
			clean_block(list);
		}
	}
	public void closeTrans(String fileName) {
		// TODO Auto-generated method stub
		if(fileName.getBytes().length<=16)//文件名最大16个字节
		{
			for(int i=0;i<inode_num_max;i++){
				int blockId = i*inode_size/block_size;
				int offset = i*inode_size%block_size;
				String string = "";
				for(int m = 0 ; m<16;m++){//组装文件名
					if(blocks[5+blockId][offset+inode_fn_offset+m]!=0)
						string+=(char)blocks[5+blockId][offset+inode_fn_offset+m];
				}
				if(fileName.equals(string)){//找到这个文件
					blocks[5+blockId][offset]=(byte)0;//改变在inode table中的文件打开状态
					List<Short> list = new ArrayList<>();//获取block下标
					for(int j=0;j<21;j++){
						short block = bytesToShort(blocks[5+blockId], offset+inode_bn_offset+j*2);
						if(block!=0)
							list.add(block);
					}
					for(short k : list){//改变在blcok bitmap 中的block打开状态
						changeBlockStatus(k);
					}
					break;//跳出循环
				}
			}
		}
	}
	
	
}
