# Raft gRPC Implementation

* ``raft.proto``: defines behaviors of gRPC server  
* ``RaftImpl``: implements behaviors of gRPC server
* ``RaftServer``: uses RaftImpl and attaches it to a server
* ``RaftClient``: implements client side code of gRPC
* ``RaftNode``: combines gRPC server and client to create a node capable of responding and sending messages
* ``RaftSystem``: the entire raft system with multiple raft nodes

Note that most logic happens in RaftNode