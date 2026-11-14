"""Writes the tiny deterministic ONNX model the Prediction tests load.

It is NOT a trained model (training is block 5). It only proves that the backend loads an
ONNX file with ONNX Runtime for Java, feeds the features in the order of the metadata and reads
one output per horizon:

    occupancy_<h> = 0.5 * occupancy_pct + h      for h in 15, 30, 45, 60

Usage, from the repository root (the output is committed next to this script together with its
metadata, model-v1.json):

    uv run --no-project --with onnx==1.19.1 python -I \
        src/test/resources/prediction/model/generate_test_model.py \
        src/test/resources/prediction/model/model-v1.onnx
"""
import sys

import onnx
from onnx import TensorProto, helper

FEATURES = ["occupancy_pct", "minute_of_day", "day_of_week", "campus_event"]
HORIZONS = [15, 30, 45, 60]

inputs = [helper.make_tensor_value_info("features", TensorProto.FLOAT, [None, len(FEATURES)])]
nodes, initializers, outputs = [], [], []
for h in HORIZONS:
    weights = [0.5] + [0.0] * (len(FEATURES) - 1)
    initializers.append(helper.make_tensor(f"w_{h}", TensorProto.FLOAT, [len(FEATURES), 1], weights))
    initializers.append(helper.make_tensor(f"b_{h}", TensorProto.FLOAT, [1], [float(h)]))
    nodes.append(helper.make_node("MatMul", ["features", f"w_{h}"], [f"m_{h}"]))
    nodes.append(helper.make_node("Add", [f"m_{h}", f"b_{h}"], [f"occupancy_{h}"]))
    outputs.append(helper.make_tensor_value_info(f"occupancy_{h}", TensorProto.FLOAT, [None, 1]))

graph = helper.make_graph(nodes, "quadrapp_test_model", inputs, outputs, initializers)
model = helper.make_model(
    graph,
    producer_name="quadrapp-tests",
    opset_imports=[helper.make_opsetid("", 13)],
)
model.ir_version = 8
onnx.checker.check_model(model)
onnx.save(model, sys.argv[1])
