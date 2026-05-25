"""
测试目标桥接模块.

因为单元文件名以数字开头无法直接 import，此模块用 importlib 加载所有被测函数.
"""

import importlib
import sys
from pathlib import Path

# 确保当前目录在 sys.path 中
_here = Path(__file__).parent
if str(_here) not in sys.path:
    sys.path.insert(0, str(_here))

# 具名单元模块映射
_modules = {
    "u01": "01_variables_types_strings",
    "u02": "02_control_flow",
    "u03": "03_functions",
    "u04": "04_data_structures",
    "u05": "05_oop",
    "u06": "06_error_handling",
    "u07": "07_file_io_and_modules",
    "u08": "08_decorators_generators",
    "u09": "09_type_system_advanced",
    "u10": "10_async_await",
    "u11": "11_testing_advanced",
    "u12": "12_logging_config",
}

_loaded = {}
for _key, _name in _modules.items():
    _loaded[_key] = importlib.import_module(_name)

# === 01 ===
calculate_discount = _loaded["u01"].calculate_discount

# === 02 ===
classify_number = _loaded["u02"].classify_number
describe_http_status = _loaded["u02"].describe_http_status

# === 03 ===
greet = _loaded["u03"].greet
create_user = _loaded["u03"].create_user
sum_all = _loaded["u03"].sum_all
build_headers = _loaded["u03"].build_headers
make_multiplier = _loaded["u03"].make_multiplier

# === 05 ===
User = _loaded["u05"].User
Dog = _loaded["u05"].Dog
Cat = _loaded["u05"].Cat
DateUtils = _loaded["u05"].DateUtils
Product = _loaded["u05"].Product
Point = _loaded["u05"].Point
Order = _loaded["u05"].Order

# === 06 ===
divide = _loaded["u06"].divide
AppError = _loaded["u06"].AppError
ValidationError = _loaded["u06"].ValidationError

# === 08 ===
fibonacci = _loaded["u08"].fibonacci
CountDown = _loaded["u08"].CountDown
chain_generators = _loaded["u08"].chain_generators
retry = _loaded["u08"].retry

# === 09 ===
Stack = _loaded["u09"].Stack
first = _loaded["u09"].first
UserRecord = _loaded["u09"].UserRecord
format_user = _loaded["u09"].format_user
request = _loaded["u09"].request
set_log_level = _loaded["u09"].set_log_level
get_value = _loaded["u09"].get_value
Bird = _loaded["u09"].Bird
Airplane = _loaded["u09"].Airplane
make_it_fly = _loaded["u09"].make_it_fly
Builder = _loaded["u09"].Builder

# === 10 ===
fetch_data = _loaded["u10"].fetch_data
fetch_with_timeout = _loaded["u10"].fetch_with_timeout

# === 12 ===
AppConfig = _loaded["u12"].AppConfig
setup_logger = _loaded["u12"].setup_logger
JsonFormatter = _loaded["u12"].JsonFormatter
load_dotenv_simple = _loaded["u12"].load_dotenv_simple
calculate_tax = _loaded["u11"].calculate_tax
PaymentService = _loaded["u11"].PaymentService
