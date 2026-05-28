def text_to_hexmod(text, hzk_path):
    with open(hzk_path, 'rb') as f:
        hzk = f.read()
    result = []
    addr = 0
    for ch in text:
        gb = ch.encode('gb2312')
        if len(gb) != 2:
            continue
        high, low = gb
        offset = ((high - 0xA1) * 94 + (low - 0xA1)) * 32
        mod = hzk[offset:offset+32]
        # 拆成两行16字节，按地址排列
        for i in range(0, 32, 16):
            line = f"{addr:02x} " + " ".join(f"{b:02x}" for b in mod[i:i+16])
            result.append(line.upper())
            addr += 16
    # 填充剩余地址为00（和示例格式一致）
    while addr < 0xF0:
        line = f"{addr:02x} " + " ".join(["00"]*16)
        result.append(line.upper())
        addr += 16
    return "\n".join(result)

text = "轻轻的我走了，正如我轻轻的招手，作别西天的云彩。那河畔的金柳，是夕阳中的新娘；波光里的艳影，在我的心头荡漾。"
print(text_to_hexmod(text, "HZK16"))