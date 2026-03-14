package com.ntros.model.world.engine.core;

import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.SimulationGridState;

public interface SimulationGridEngine extends GridEngine {

  WorldResult orchestrate(OrchestrateRequest req, SimulationGridState state);

}
