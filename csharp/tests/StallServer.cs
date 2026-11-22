using System.Net;
using System.Net.Sockets;

namespace Smritea.Sdk.Tests;

/// <summary>How a <see cref="StallServer"/> misbehaves once a client connects.</summary>
public enum StallMode
{
    /// <summary>Accepts the connection and never writes a byte.</summary>
    NeverAnswers,

    /// <summary>Reads the request, writes a 200 status line with a partial body, then stalls.</summary>
    StalledBody,
}

/// <summary>
/// A real loopback TCP server that stalls on purpose, so tests can prove request timeouts
/// against an actual socket instead of a mocked handler.
/// </summary>
public sealed class StallServer : IDisposable
{
    private const int DeclaredBodyLength = 100;
    private const int SentBodyLength = 10;
    private const int ReadBufferSize = 4096;
    private const string HeaderEnd = "\r\n\r\n";

    private readonly TcpListener _listener;
    private readonly CancellationTokenSource _stop = new();
    private readonly StallMode _mode;

    public StallServer(StallMode mode)
    {
        _mode = mode;
        _listener = new TcpListener(IPAddress.Loopback, 0);
        _listener.Start();
        Url = $"http://127.0.0.1:{((IPEndPoint)_listener.LocalEndpoint).Port}";
        AcceptTask = AcceptLoopAsync();
    }

    public string Url { get; }

    private Task AcceptTask { get; }

    public void Dispose()
    {
        _stop.Cancel();
        _listener.Stop();
        _stop.Dispose();
    }

    private async Task AcceptLoopAsync()
    {
        try
        {
            while (!_stop.IsCancellationRequested)
            {
                var tcp = await _listener.AcceptTcpClientAsync(_stop.Token);
                _ = HandleAsync(tcp);
            }
        }
        catch (OperationCanceledException)
        {
            // Dispose stops the accept loop.
        }
        catch (ObjectDisposedException)
        {
            // The listener was stopped while an accept was pending.
        }
        catch (SocketException)
        {
            // The listener was stopped while an accept was pending.
        }
    }

    private async Task HandleAsync(TcpClient tcp)
    {
        try
        {
            using (tcp)
            {
                if (_mode == StallMode.StalledBody)
                {
                    var stream = tcp.GetStream();
                    await ReadUntilHeadersEndAsync(stream);
                    var head = $"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: {DeclaredBodyLength}\r\n\r\n"
                        + new string('x', SentBodyLength);
                    var bytes = System.Text.Encoding.ASCII.GetBytes(head);
                    await stream.WriteAsync(bytes, _stop.Token);
                    await stream.FlushAsync(_stop.Token);
                }

                await Task.Delay(Timeout.Infinite, _stop.Token);
            }
        }
        catch (OperationCanceledException)
        {
            // Dispose releases every stalled connection.
        }
        catch (IOException)
        {
            // The client gave up and closed the connection.
        }
        catch (ObjectDisposedException)
        {
            // The connection was closed while a read or write was pending.
        }
    }

    private async Task ReadUntilHeadersEndAsync(NetworkStream stream)
    {
        var buffer = new byte[ReadBufferSize];
        var received = new System.Text.StringBuilder();
        while (!received.ToString().Contains(HeaderEnd, StringComparison.Ordinal))
        {
            var read = await stream.ReadAsync(buffer, _stop.Token);
            if (read == 0)
            {
                return;
            }

            received.Append(System.Text.Encoding.ASCII.GetString(buffer, 0, read));
        }
    }
}
